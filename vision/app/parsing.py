"""Turns raw OCR text into structured event payloads. Pure functions, no
image/OCR dependency — the highest-value tests in this service per the same
philosophy as core's rules engine: deterministic, fast, no mocks needed.
Returns None on anything that doesn't look like the expected shape rather
than raising, since OCR misreads are the normal case, not an error.
"""

import re

_SCORE_PATTERN = re.compile(r"(\d{1,3})\D+(\d{1,3})")
_CLOCK_PATTERN = re.compile(r"(\d{1,2}):(\d{2})")
_SHOT_CLOCK_PATTERN = re.compile(r"\b(\d{1,2})\b")


def parse_score(text: str) -> dict[str, int] | None:
    match = _SCORE_PATTERN.search(text)
    if not match:
        return None
    return {"teamAScore": int(match.group(1)), "teamBScore": int(match.group(2))}


def parse_clock(text: str) -> dict[str, int] | None:
    match = _CLOCK_PATTERN.search(text)
    if not match:
        return None
    minutes, seconds = int(match.group(1)), int(match.group(2))
    if seconds > 59:
        return None
    return {"minutes": minutes, "seconds": seconds}


def parse_shot_clock(text: str) -> dict[str, int] | None:
    stripped = text.strip()
    match = _SHOT_CLOCK_PATTERN.search(stripped)
    if not match:
        return None
    seconds = int(match.group(1))
    if seconds > 24:
        return None
    return {"secondsRemaining": seconds}
