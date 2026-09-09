"""Turns raw OCR text into structured event payloads. Pure functions, no
image/OCR dependency — the highest-value tests in this service per the same
philosophy as core's rules engine: deterministic, fast, no mocks needed.
Returns None on anything that doesn't look like the expected shape rather
than raising, since OCR misreads are the normal case, not an error.
"""

import re

_SCORE_PATTERN = re.compile(r"(\d{1,3})\D+(\d{1,3})")
# One digit after the separator: NBA 2K switches the game/shot clock to a
# seconds.tenths display once time drops under a minute (confirmed against a
# real in-game screenshot showing "41.2" for 41.2 seconds left, not "41
# minutes 2 seconds"). Two digits after the separator: the normal M:SS
# display. OCR commonly misreads the separator itself as a period or comma
# instead of a colon (confirmed against EasyOCR on a synthetic "5:23"
# fixture, which read back "5.23") — the character used isn't meaningfully
# part of the value, only the digit count on each side is.
_CLOCK_PATTERN = re.compile(r"\b(\d{1,2})[:.,](\d{1,2})\b")
_SHOT_CLOCK_PATTERN = re.compile(r"\b(\d{1,2})\b")
_QUARTER_PATTERN = re.compile(r"\b(1st|2nd|3rd|4th|OT\d?)\b", re.IGNORECASE)


def parse_score(text: str) -> dict[str, int] | None:
    match = _SCORE_PATTERN.search(text)
    if not match:
        return None
    return {"teamAScore": int(match.group(1)), "teamBScore": int(match.group(2))}


def parse_clock(text: str) -> dict[str, int] | None:
    match = _CLOCK_PATTERN.search(text)
    if not match:
        return None
    first, second = match.group(1), match.group(2)
    if len(second) == 2:
        minutes, seconds = int(first), int(second)
        if seconds > 59:
            return None
        return {"minutes": minutes, "seconds": seconds}
    # Single digit after the separator: "41.2" is 41.2 seconds, not minutes.
    seconds = int(first)
    if seconds > 59:
        return None
    return {"minutes": 0, "seconds": seconds}


def parse_shot_clock(text: str) -> dict[str, int] | None:
    stripped = text.strip()
    match = _SHOT_CLOCK_PATTERN.search(stripped)
    if not match:
        return None
    seconds = int(match.group(1))
    if seconds > 24:
        return None
    return {"secondsRemaining": seconds}


def parse_quarter(text: str) -> dict[str, str] | None:
    match = _QUARTER_PATTERN.search(text)
    if not match:
        return None
    return {"quarter": match.group(1).upper() if match.group(1).upper().startswith("OT") else match.group(1).lower()}
