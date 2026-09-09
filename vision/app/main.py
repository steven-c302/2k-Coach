"""nba2k-assistant-vision entrypoint.

Deliberately last in the roadmap (plan §7, Milestone 10) — it only emits
observed facts to core (plan §1: "no game-domain logic"). One CaptureLoop
runs per session, started/stopped explicitly rather than automatically —
there's no reliable signal in this environment for "a game just started."
"""

from fastapi import FastAPI
from pydantic import BaseModel

from app.capture.loop import CaptureLoop
from app.capture.regions import DEFAULT_PROFILE
from app.config import settings

app = FastAPI(title="nba2k-assistant-vision")

_active_loops: dict[str, CaptureLoop] = {}


class StartCaptureRequest(BaseModel):
    session_id: str


class SessionIdRequest(BaseModel):
    session_id: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/api/capture/status")
def capture_status() -> dict[str, object]:
    return {
        "activeSessions": [session_id for session_id, loop in _active_loops.items() if loop.running],
        "backend": settings.capture_backend,
        "core_base_url": settings.core_base_url,
    }


@app.post("/api/capture/start")
async def start_capture(request: StartCaptureRequest) -> dict[str, str]:
    existing = _active_loops.get(request.session_id)
    if existing is not None and existing.running:
        return {"status": "already running"}

    loop = CaptureLoop(session_id=request.session_id, profile=DEFAULT_PROFILE)
    loop.start()
    _active_loops[request.session_id] = loop
    return {"status": "started"}


@app.post("/api/capture/stop")
def stop_capture(request: SessionIdRequest) -> dict[str, str]:
    loop = _active_loops.pop(request.session_id, None)
    if loop is None:
        return {"status": "not running"}
    loop.stop()
    return {"status": "stopped"}
