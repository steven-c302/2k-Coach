"""nba2k-assistant-vision entrypoint.

Deliberately last in the roadmap (plan §7, Milestone 10) — it only emits
observed facts to core (plan §1: "no game-domain logic"), so it can't be
demoed meaningfully until core's session/coaching pipeline exists. For now
this exposes health/status only; the capture -> OpenCV -> EasyOCR -> emit loop
gets built once Milestones 1-9 are working.
"""

from fastapi import FastAPI

from app.config import settings

app = FastAPI(title="nba2k-assistant-vision")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/api/capture/status")
def capture_status() -> dict[str, object]:
    return {
        "running": False,
        "backend": settings.capture_backend,
        "core_base_url": settings.core_base_url,
        "note": "capture loop not yet implemented — see app/capture/regions.py",
    }
