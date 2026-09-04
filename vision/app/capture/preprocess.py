"""OpenCV preprocessing: crop to a calibrated HUD region, grayscale, threshold,
upscale — per plan §6, "this step is where most of the accuracy work
happens." The crop/resize math is verified by unit tests against synthetic
arrays; the actual accuracy tuning needs a real capture (see
app/capture/regions.py) and can't happen in this environment.
"""

import cv2
import numpy as np

from app.capture.regions import Region


def preprocess_region(frame: np.ndarray, region: Region, upscale: int = 3) -> np.ndarray:
    cropped = frame[region.y : region.y + region.height, region.x : region.x + region.width]
    if cropped.size == 0:
        raise ValueError(f"Region {region} is empty or out of bounds for a frame of shape {frame.shape}")

    if cropped.ndim == 3 and cropped.shape[2] == 4:
        gray = cv2.cvtColor(cropped, cv2.COLOR_BGRA2GRAY)
    elif cropped.ndim == 3:
        gray = cv2.cvtColor(cropped, cv2.COLOR_BGR2GRAY)
    else:
        gray = cropped

    _, thresholded = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    return cv2.resize(thresholded, None, fx=upscale, fy=upscale, interpolation=cv2.INTER_CUBIC)
