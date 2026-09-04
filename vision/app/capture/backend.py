"""Screen capture via mss (plan §6: "simple, portable" for V1; note
bettercam/dxcam as a drop-in perf upgrade later — swapping the capture
library doesn't touch downstream code since everything past this module
only depends on getting a BGRA numpy array back).

Needs a real, attached display — will raise inside a headless container or
CI runner. That's expected: this only ever runs meaningfully on the machine
actually playing the game (see the project README's dev-environment notes).
"""

import mss
import numpy as np


def grab_frame(monitor_index: int = 1) -> np.ndarray:
    with mss.mss() as sct:
        raw = sct.grab(sct.monitors[monitor_index])
        return np.array(raw)
