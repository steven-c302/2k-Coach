"""Event shape shared with core's POST /api/vision/events (plan §6) and with the
manual tap-tracker in web — both flow through the same endpoint so
the coaching engine sees one unified stream regardless of source.
"""

from datetime import datetime, timezone
from enum import Enum
from typing import Any

import httpx
from pydantic import BaseModel, ConfigDict, Field

from app.config import settings


class VisionEventType(str, Enum):
    SCORE_UPDATE = "SCORE_UPDATE"
    CLOCK_UPDATE = "CLOCK_UPDATE"
    BOX_SCORE_UPDATE = "BOX_SCORE_UPDATE"


class VisionEvent(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    session_id: str = Field(alias="sessionId")
    type: VisionEventType
    payload: dict[str, Any]
    captured_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc), alias="capturedAt")


class EventClient:
    """Thin POST wrapper. Vision never touches Postgres/DynamoDB directly —
    core is the only writer (plan §1) — so this is the entire vision->core
    integration surface. `transport` is injectable so tests can use
    httpx.MockTransport instead of a real network call.
    """

    def __init__(self, base_url: str = settings.core_base_url, transport: httpx.BaseTransport | None = None) -> None:
        self._base_url = base_url
        self._transport = transport

    async def emit(self, event: VisionEvent) -> None:
        async with httpx.AsyncClient(base_url=self._base_url, timeout=5.0, transport=self._transport) as client:
            response = await client.post(
                "/api/vision/events",
                json=event.model_dump(mode="json", by_alias=True),
            )
            response.raise_for_status()
