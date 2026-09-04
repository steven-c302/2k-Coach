"""EasyOCR wrapper (plan §6: "better out-of-box accuracy on stylized fonts
than Tesseract" — the explicit tradeoff against latency/dependency weight).
The reader loads its model weights on first use and is reused after that —
constructing a new Reader per call would re-load weights every tick.

`easyocr` (and its torch dependency) is imported lazily inside get_reader(),
not at module level: this module is on the import chain from app.main (via
app.capture.loop), and importing it eagerly would force every consumer of
app.main — including CI's lightweight test job, which deliberately skips the
large ML stack — to have easyocr installed just to collect tests that never
call this code.
"""

from typing import TYPE_CHECKING

import numpy as np

if TYPE_CHECKING:
    import easyocr

_reader: "easyocr.Reader | None" = None


def get_reader(gpu: bool = False) -> "easyocr.Reader":
    global _reader
    if _reader is None:
        import easyocr

        _reader = easyocr.Reader(["en"], gpu=gpu)
    return _reader


def read_text(image: np.ndarray, gpu: bool = False) -> str:
    reader = get_reader(gpu=gpu)
    results = reader.readtext(image, detail=0)
    return " ".join(results).strip()
