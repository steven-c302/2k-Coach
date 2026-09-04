import asyncio
import json

import httpx
import pytest

from app.events import EventClient, VisionEvent, VisionEventType


def test_emit_posts_the_event_shape_core_expects():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["body"] = json.loads(request.content)
        return httpx.Response(200)

    client = EventClient(base_url="http://core:8080", transport=httpx.MockTransport(handler))
    event = VisionEvent(session_id="ABC123", type=VisionEventType.SCORE_UPDATE, payload={"teamAScore": 5, "teamBScore": 3})

    asyncio.run(client.emit(event))

    assert captured["url"] == "http://core:8080/api/vision/events"
    assert captured["body"]["sessionId"] == "ABC123"
    assert captured["body"]["type"] == "SCORE_UPDATE"
    assert captured["body"]["payload"] == {"teamAScore": 5, "teamBScore": 3}


def test_emit_raises_on_a_non_2xx_response():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500)

    client = EventClient(base_url="http://core:8080", transport=httpx.MockTransport(handler))
    event = VisionEvent(session_id="ABC123", type=VisionEventType.SCORE_UPDATE, payload={})

    with pytest.raises(httpx.HTTPStatusError):
        asyncio.run(client.emit(event))
