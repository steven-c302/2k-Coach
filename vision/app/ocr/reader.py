"""EasyOCR wrapper (plan §6: "better out-of-box accuracy on stylized fonts
than Tesseract" — the explicit tradeoff against latency/dependency weight).
The reader loads its model weights on first use and is reused after that —
constructing a new Reader per call would re-load weights every tick.
"""

import easyocr
import numpy as np

_reader: easyocr.Reader | None = None


def get_reader(gpu: bool = False) -> easyocr.Reader:
    global _reader
    if _reader is None:
        _reader = easyocr.Reader(["en"], gpu=gpu)
    return _reader


def read_text(image: np.ndarray, gpu: bool = False) -> str:
    reader = get_reader(gpu=gpu)
    results = reader.readtext(image, detail=0)
    return " ".join(results).strip()
