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
from app.parsing import parse_clock, parse_quarter, parse_shot_clock, parse_single_score

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

        def region_text(fractional_region, allowlist: str | None = None, upscale: int = 3) -> str:
            pixel_region = fractional_region.to_pixels(frame_width, frame_height)
            processed = preprocess.preprocess_region(frame, pixel_region, upscale=upscale)
            return read_text(processed, gpu=settings.ocr_use_gpu, allowlist=allowlist)

        # allowlist restricts recognized characters - digits-only for regions that can only ever
        # contain digits eliminates letter/digit shape confusion (confirmed against real HUD
        # screenshots: "1st" misread as "Ist" was fixed by excluding "I" from what quarter can
        # recognize at all). Two separate single-number score crops, not one combined region - see
        # regions.py for why.
        team_a_score = parse_single_score(region_text(self.profile.team_a_score, allowlist="0123456789"))
        team_b_score = parse_single_score(region_text(self.profile.team_b_score, allowlist="0123456789"))
        score_payload = {
            **({"teamAScore": team_a_score} if team_a_score is not None else {}),
            **({"teamBScore": team_b_score} if team_b_score is not None else {}),
        }
        if score_payload:
            await self._event_client.emit(VisionEvent(
                session_id=self.session_id, type=VisionEventType.SCORE_UPDATE, payload=score_payload,
            ))

        # upscale=7: the game clock's separator character ("." under a minute, ":" over) is small
        # enough that OCR's text-detection stage misses it entirely at the default upscale - a
        # higher upscale made it detectable, confirmed against real screenshots. This doesn't need
        # the second digit after the separator to also be perfectly recognized: parse_clock only
        # uses it to tell the two display formats apart (1 digit = tenths, discarded; 2 digits =
        # seconds, used), not as a value in its own right.
        clock = parse_clock(region_text(self.profile.game_clock, allowlist="0123456789:.,", upscale=7))
        shot_clock = parse_shot_clock(region_text(self.profile.shot_clock, allowlist="0123456789"))
        quarter = parse_quarter(region_text(self.profile.quarter, allowlist="0123456789stndrhOT"))
        clock_payload = {
            **(clock or {}),
            **({"shotClockSecondsRemaining": shot_clock["secondsRemaining"]} if shot_clock else {}),
            **(quarter or {}),
        }
        if clock_payload:
            await self._event_client.emit(VisionEvent(
                session_id=self.session_id, type=VisionEventType.CLOCK_UPDATE, payload=clock_payload,
            ))
