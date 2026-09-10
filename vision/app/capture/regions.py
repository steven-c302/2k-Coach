"""HUD region calibration.

Regions are fractions (0-1) of the captured frame's width/height, not fixed
pixel offsets — comparing real gameplay screenshots across two different
broadcast camera angles and two different capture aspect ratios (a
~2.45:1 ultrawide-style capture and a ~1.78:1 16:9 one) showed the bottom
scoreboard bar (score/game clock/shot clock/quarter) sitting at the same
fractional position in both, so one profile scales to whatever resolution
grab_frame() actually returns via to_pixels() — no per-resolution profile
table, and no "unsupported resolution" failure mode.

The DEFAULT_PROFILE fractions below are measured against four real 3440x1440
gameplay screenshots (vision/tests/fixtures/hud_samples) by cropping
candidate regions, running them through the real OCR pipeline, and comparing
against the actual on-screen values — see test_hud_samples.py for that as an
automated regression check. box_score has no real reference screenshot yet
and is an unverified placeholder guess at a bottom stats overlay.
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class Region:
    x: int
    y: int
    width: int
    height: int


@dataclass(frozen=True)
class FractionalRegion:
    """A bounding box as a fraction (0-1) of the frame's width/height."""

    x: float
    y: float
    width: float
    height: float

    def to_pixels(self, frame_width: int, frame_height: int) -> Region:
        return Region(
            x=round(self.x * frame_width),
            y=round(self.y * frame_height),
            width=round(self.width * frame_width),
            height=round(self.height * frame_height),
        )


@dataclass(frozen=True)
class HudProfile:
    # team_a_score/team_b_score are separate regions, not one combined "score" box spanning both
    # team logos - a combined crop reliably corrupted the OCR read (team logos between the two
    # numbers got misread as extra digits, e.g. "122 ... 127" came back as "222"/"440"), confirmed
    # against all four real screenshots. Two clean single-number crops fixed it completely.
    team_a_score: FractionalRegion
    team_b_score: FractionalRegion
    game_clock: FractionalRegion
    shot_clock: FractionalRegion
    quarter: FractionalRegion
    box_score: FractionalRegion


DEFAULT_PROFILE = HudProfile(
    team_a_score=FractionalRegion(x=0.2020, y=0.9299, width=0.0436, height=0.0361),
    team_b_score=FractionalRegion(x=0.3096, y=0.9299, width=0.0407, height=0.0361),
    game_clock=FractionalRegion(x=0.4041, y=0.9299, width=0.0256, height=0.0361),
    shot_clock=FractionalRegion(x=0.4404, y=0.9299, width=0.0305, height=0.0361),
    quarter=FractionalRegion(x=0.4695, y=0.9299, width=0.0276, height=0.0361),
    box_score=FractionalRegion(x=0.0, y=0.85, width=1.0, height=0.15),
)
