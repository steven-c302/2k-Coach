import pytest

from app.capture.regions import profile_for


def test_profile_for_known_resolution():
    profile = profile_for((1920, 1080))

    assert profile.resolution == (1920, 1080)


def test_profile_for_unknown_resolution_raises():
    with pytest.raises(ValueError):
        profile_for((1280, 720))
