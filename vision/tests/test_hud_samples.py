"""Regression test against real NBA 2K gameplay screenshots (not synthetic
cv2.putText fixtures like test_ocr_golden.py) — this is what DEFAULT_PROFILE
in regions.py is actually calibrated against. Slow (real EasyOCR inference,
same as test_ocr_golden.py) — skipped by CI's default `-m "not slow"` run.

Two real HUD skins are covered: 20260702143608_*.jpg (gameplay camera, the
sub-minute "SS.T" clock format) and 20260909*.jpg (broadcast camera, the
normal "M:SS" clock format).

Known, currently-unresolved gaps, left undocumented as passing assertions
rather than papered over: team_a_score misreads "122" on the
20260702143608_* pair specifically (confirmed against a visually clean,
correctly-cropped image — an EasyOCR model limitation on this exact
rendering, not a region or allowlist fix, per regions.py/loop.py) and
game_clock doesn't yet reliably recognize the "M:SS" separator on the
20260909* pair (only the "SS.T" pair is asserted below). Both are why this
suite exists — to catch it if a future change makes things worse, and get
un-skipped as soon as either is actually fixed.
"""

import os

import cv2
import numpy as np
import pytest
from PIL import Image

from app.capture.preprocess import preprocess_region
from app.capture.regions import DEFAULT_PROFILE
from app.ocr.reader import read_text
from app.parsing import parse_clock, parse_quarter, parse_shot_clock, parse_single_score

pytestmark = pytest.mark.slow

FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "fixtures", "hud_samples")


def _load_frame(filename: str) -> np.ndarray:
    image = Image.open(os.path.join(FIXTURES_DIR, filename)).convert("RGB")
    return cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)


def _ocr_region(frame: np.ndarray, fractional_region, allowlist: str, upscale: int = 3) -> str:
    frame_height, frame_width = frame.shape[0], frame.shape[1]
    region = fractional_region.to_pixels(frame_width, frame_height)
    processed = preprocess_region(frame, region, upscale=upscale)
    return read_text(processed, gpu=False, allowlist=allowlist)


@pytest.mark.parametrize("filename", ["20260702143608_1.jpg", "20260702143608_2.jpg"])
def test_shot_clock_and_quarter_read_correctly_on_the_gameplay_camera_skin(filename):
    frame = _load_frame(filename)

    shot_clock_text = _ocr_region(frame, DEFAULT_PROFILE.shot_clock, allowlist="0123456789")
    assert parse_shot_clock(shot_clock_text) == {"secondsRemaining": 24}

    quarter_text = _ocr_region(frame, DEFAULT_PROFILE.quarter, allowlist="0123456789stndrhOT")
    assert parse_quarter(quarter_text) == {"quarter": "4th"}

    team_b_text = _ocr_region(frame, DEFAULT_PROFILE.team_b_score, allowlist="0123456789")
    assert parse_single_score(team_b_text) == 127


@pytest.mark.parametrize("filename", ["20260702143608_1.jpg", "20260702143608_2.jpg"])
def test_game_clock_reads_the_under_a_minute_format_on_the_gameplay_camera_skin(filename):
    frame = _load_frame(filename)

    clock_text = _ocr_region(frame, DEFAULT_PROFILE.game_clock, allowlist="0123456789:.,", upscale=7)

    parsed = parse_clock(clock_text)
    assert parsed is not None
    assert parsed["minutes"] == 0
    assert parsed["seconds"] == 41


@pytest.mark.parametrize(
    "filename,expected_shot_clock,expected_quarter",
    [
        ("20260909115943_1.jpg", 6, "1st"),
        ("20260909120000_1.jpg", 16, "1st"),
    ],
)
def test_shot_clock_and_quarter_read_correctly_on_the_broadcast_camera_skin(
    filename, expected_shot_clock, expected_quarter
):
    frame = _load_frame(filename)

    shot_clock_text = _ocr_region(frame, DEFAULT_PROFILE.shot_clock, allowlist="0123456789")
    assert parse_shot_clock(shot_clock_text) == {"secondsRemaining": expected_shot_clock}

    quarter_text = _ocr_region(frame, DEFAULT_PROFILE.quarter, allowlist="0123456789stndrhOT")
    assert parse_quarter(quarter_text) == {"quarter": expected_quarter}


@pytest.mark.parametrize(
    "filename,expected_a,expected_b",
    [
        ("20260909115943_1.jpg", 0, 0),
        ("20260909120000_1.jpg", 0, 2),
    ],
)
def test_score_reads_correctly_on_the_broadcast_camera_skin(filename, expected_a, expected_b):
    frame = _load_frame(filename)

    team_a_text = _ocr_region(frame, DEFAULT_PROFILE.team_a_score, allowlist="0123456789")
    team_b_text = _ocr_region(frame, DEFAULT_PROFILE.team_b_score, allowlist="0123456789")

    assert parse_single_score(team_a_text) == expected_a
    assert parse_single_score(team_b_text) == expected_b
