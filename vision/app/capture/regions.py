"""HUD region calibration.

Regions are fractions (0-1) of the captured frame's width/height, not fixed
pixel offsets — comparing real gameplay screenshots across two different
broadcast camera angles and two different capture aspect ratios (a
~2.45:1 ultrawide-style capture and a ~1.78:1 16:9 one) showed the bottom
scoreboard bar (score/game clock/shot clock/quarter) sitting at the same
fractional position in both, so one profile scales to whatever resolution
grab_frame() actually returns via to_pixels() — no per-resolution profile
table, and no "unsupported resolution" failure mode.

The DEFAULT_PROFILE fractions below are a first calibration pass read off
four real screenshots (not synthetic), not yet tuned against a live OCR
run — treat them as a starting point to nudge from real capture results,
not as final. box_score has no real reference screenshot yet and is an
unverified placeholder guess at a bottom stats overlay.
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
    score: FractionalRegion
    game_clock: FractionalRegion
    shot_clock: FractionalRegion
    quarter: FractionalRegion
    box_score: FractionalRegion


DEFAULT_PROFILE = HudProfile(
    score=FractionalRegion(x=0.085, y=0.94, width=0.22, height=0.06),
    game_clock=FractionalRegion(x=0.39, y=0.94, width=0.075, height=0.06),
    shot_clock=FractionalRegion(x=0.463, y=0.94, width=0.04, height=0.06),
    quarter=FractionalRegion(x=0.503, y=0.94, width=0.045, height=0.06),
    box_score=FractionalRegion(x=0.0, y=0.85, width=1.0, height=0.15),
)
