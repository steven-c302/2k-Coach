"""Golden-file style per plan §8 — "pytest against a fixture set of captured
screenshots with known ground-truth values." No real NBA 2K captures exist
in this environment, so the fixture is a synthetic rendered image instead:
it proves the OCR wrapper and the parsing functions correctly compose end to
end, but is not a substitute for testing against the real stylized 2K HUD
font — that needs a real capture on your PC (see the README's known gaps).

Marked slow: needs EasyOCR's real model weights (torch), skipped by CI's
lightweight `vision-tests` job. Run locally (`pip install -r
requirements.txt && pytest`) or inside the vision Docker image to exercise
this for real.
"""

import cv2
import numpy as np
import pytest

from app.ocr.reader import read_text
from app.parsing import parse_clock, parse_score


def _render_text(text: str, width: int = 300, height: int = 80) -> np.ndarray:
    image = np.full((height, width, 3), 255, dtype=np.uint8)
    cv2.putText(image, text, (10, height - 20), cv2.FONT_HERSHEY_SIMPLEX, 1.8, (0, 0, 0), 3)
    return image


@pytest.mark.slow
def test_ocr_reads_a_rendered_score():
    image = _render_text("72 68")

    text = read_text(image)

    assert parse_score(text) == {"teamAScore": 72, "teamBScore": 68}


@pytest.mark.slow
def test_ocr_reads_a_rendered_clock():
    image = _render_text("5:23")

    text = read_text(image)

    assert parse_clock(text) == {"minutes": 5, "seconds": 23}
