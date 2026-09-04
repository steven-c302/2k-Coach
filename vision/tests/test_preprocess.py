import numpy as np
import pytest

from app.capture.preprocess import preprocess_region
from app.capture.regions import Region


def test_preprocess_crops_grayscales_and_upscales():
    frame = np.zeros((200, 200, 4), dtype=np.uint8)  # BGRA, like an mss capture
    region = Region(x=10, y=10, width=40, height=20)

    result = preprocess_region(frame, region, upscale=3)

    assert result.shape == (20 * 3, 40 * 3)  # grayscale: no channel dimension
    assert result.dtype == np.uint8


def test_preprocess_handles_bgr_frames_without_an_alpha_channel():
    frame = np.zeros((100, 100, 3), dtype=np.uint8)
    region = Region(x=0, y=0, width=50, height=50)

    result = preprocess_region(frame, region, upscale=1)

    assert result.shape == (50, 50)


def test_preprocess_raises_on_an_out_of_bounds_region():
    frame = np.zeros((50, 50, 4), dtype=np.uint8)
    region = Region(x=100, y=100, width=20, height=20)

    with pytest.raises(ValueError):
        preprocess_region(frame, region)
