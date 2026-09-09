from app.capture.regions import DEFAULT_PROFILE, FractionalRegion, Region


def test_fractional_region_converts_to_pixels_for_a_given_frame_size():
    region = FractionalRegion(x=0.1, y=0.2, width=0.3, height=0.4)

    pixels = region.to_pixels(frame_width=1000, frame_height=500)

    assert pixels == Region(x=100, y=100, width=300, height=200)


def test_fractional_region_scales_to_a_different_frame_size():
    region = FractionalRegion(x=0.5, y=0.5, width=0.1, height=0.1)

    pixels_1080p = region.to_pixels(frame_width=1920, frame_height=1080)
    pixels_ultrawide = region.to_pixels(frame_width=2560, frame_height=1080)

    # Same fractional box, different absolute width for a wider capture -
    # this is the whole point: one profile, no per-resolution table.
    assert pixels_1080p.width == 192
    assert pixels_ultrawide.width == 256
    assert pixels_1080p.height == pixels_ultrawide.height == 108


def test_default_profile_regions_stay_within_frame_bounds():
    for region in (
        DEFAULT_PROFILE.score,
        DEFAULT_PROFILE.game_clock,
        DEFAULT_PROFILE.shot_clock,
        DEFAULT_PROFILE.quarter,
        DEFAULT_PROFILE.box_score,
    ):
        assert 0 <= region.x <= 1
        assert 0 <= region.y <= 1
        assert 0 < region.width <= 1
        assert 0 < region.height <= 1
        assert region.x + region.width <= 1.0001  # tiny slack for float rounding
        assert region.y + region.height <= 1.0001
