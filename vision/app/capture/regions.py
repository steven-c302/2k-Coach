"""HUD region calibration profiles.

Per plan §6, coordinates are pixel offsets within the captured frame and are
resolution-specific — the accuracy work happens here, sitting at the PC with a
real "Play Now" game running, not something to guess at from a spec. The one
profile below is an unverified placeholder for 1920x1080 to unblock wiring the
rest of the pipeline (Milestone 10); it must be re-measured against a real
capture before OCR results mean anything.
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class Region:
    x: int
    y: int
    width: int
    height: int


@dataclass(frozen=True)
class HudProfile:
    resolution: tuple[int, int]
    score: Region
    game_clock: Region
    shot_clock: Region
    box_score: Region


# TODO(Milestone 10): replace with measured coordinates from an actual capture.
PLACEHOLDER_1080P = HudProfile(
    resolution=(1920, 1080),
    score=Region(x=760, y=20, width=400, height=60),
    game_clock=Region(x=900, y=20, width=120, height=40),
    shot_clock=Region(x=1040, y=20, width=80, height=40),
    box_score=Region(x=0, y=900, width=1920, height=180),
)

PROFILES: dict[tuple[int, int], HudProfile] = {
    PLACEHOLDER_1080P.resolution: PLACEHOLDER_1080P,
}


def profile_for(resolution: tuple[int, int]) -> HudProfile:
    try:
        return PROFILES[resolution]
    except KeyError as exc:
        raise ValueError(f"No calibration profile for resolution {resolution}") from exc
