"""Start/stop-controlled capture loop — one per session, ticking on an
interval (plan §6: "polled every 1-2s; values change slowly; no need for
frame-rate OCR"). A tick failure (e.g. no display attached, a bad region,
a transient OCR error) is logged and skipped rather than killing the loop —
one missed reading is fine, dying because of it isn't.
"""

import asyncio
import logging

from app.capture import backend, preprocess
from app.capture.regions import HudProfile
from app.config import settings
from app.events import EventClient, VisionEvent, VisionEventType
from app.ocr.reader import read_text
from app.parsing import parse_clock, parse_quarter, parse_score, parse_shot_clock

logger = logging.getLogger(__name__)


class CaptureLoop:
    def __init__(self, session_id: str, profile: HudProfile, event_client: EventClient | None = None) -> None:
        self.session_id = session_id
        self.profile = profile
        self._event_client = event_client or EventClient()
        self._task: asyncio.Task | None = None
        self._running = False

    @property
    def running(self) -> bool:
        return self._running

    def start(self) -> None:
        if self._running:
            return
        self._running = True
        self._task = asyncio.create_task(self._run())

    def stop(self) -> None:
        self._running = False
        if self._task is not None:
            self._task.cancel()
            self._task = None

    async def _run(self) -> None:
        while self._running:
            try:
                await self.tick()
            except Exception:
                logger.exception("Capture tick failed for session %s", self.session_id)
            await asyncio.sleep(settings.capture_interval_seconds)

    async def tick(self) -> None:
        """One capture-preprocess-OCR-parse-emit cycle. Public and awaited
        directly (not just via the interval loop) so it's independently
        testable without needing to wait out a real sleep interval."""
        frame = backend.grab_frame()
        frame_height, frame_width = frame.shape[0], frame.shape[1]

        def region_text(fractional_region) -> str:
            pixel_region = fractional_region.to_pixels(frame_width, frame_height)
            return read_text(preprocess.preprocess_region(frame, pixel_region), gpu=settings.ocr_use_gpu)

        score = parse_score(region_text(self.profile.score))
        if score is not None:
            await self._event_client.emit(VisionEvent(
                session_id=self.session_id, type=VisionEventType.SCORE_UPDATE, payload=score,
            ))

        clock = parse_clock(region_text(self.profile.game_clock))
        shot_clock = parse_shot_clock(region_text(self.profile.shot_clock))
        quarter = parse_quarter(region_text(self.profile.quarter))
        clock_payload = {
            **(clock or {}),
            **({"shotClockSecondsRemaining": shot_clock["secondsRemaining"]} if shot_clock else {}),
            **(quarter or {}),
        }
        if clock_payload:
            await self._event_client.emit(VisionEvent(
                session_id=self.session_id, type=VisionEventType.CLOCK_UPDATE, payload=clock_payload,
            ))
