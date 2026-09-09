from app.parsing import parse_clock, parse_quarter, parse_score, parse_shot_clock


def test_parse_score_reads_two_numbers_separated_by_noise():
    assert parse_score("72 - 68") == {"teamAScore": 72, "teamBScore": 68}
    assert parse_score("72-68") == {"teamAScore": 72, "teamBScore": 68}
    assert parse_score("Score: 5 to 3") == {"teamAScore": 5, "teamBScore": 3}


def test_parse_score_returns_none_when_no_score_shape_is_found():
    assert parse_score("PAUSED") is None
    assert parse_score("") is None
    assert parse_score("99") is None


def test_parse_clock_reads_minutes_and_seconds():
    assert parse_clock("5:23") == {"minutes": 5, "seconds": 23}
    assert parse_clock("Q3 05:23") == {"minutes": 5, "seconds": 23}


def test_parse_clock_tolerates_a_colon_misread_as_a_period_or_comma():
    # Confirmed against real EasyOCR output on a synthetic "5:23" fixture,
    # which read back "5.23" — see test_ocr_golden.py.
    assert parse_clock("5.23") == {"minutes": 5, "seconds": 23}
    assert parse_clock("5,23") == {"minutes": 5, "seconds": 23}


def test_parse_clock_rejects_an_invalid_seconds_value():
    assert parse_clock("5:99") is None


def test_parse_clock_returns_none_without_a_colon():
    assert parse_clock("523") is None


def test_parse_clock_reads_the_under_a_minute_seconds_tenths_display():
    # Confirmed against a real in-game screenshot: NBA 2K's clock shows
    # "41.2" (41.2 seconds left) once time drops under a minute, not "41
    # minutes 2 seconds" - the single digit after the separator is what
    # distinguishes this from the normal M:SS display.
    assert parse_clock("41.2") == {"minutes": 0, "seconds": 41}
    assert parse_clock("5.3") == {"minutes": 0, "seconds": 5}


def test_parse_clock_rejects_an_invalid_under_a_minute_value():
    assert parse_clock("65.2") is None


def test_parse_quarter_reads_ordinal_periods():
    assert parse_quarter("4th") == {"quarter": "4th"}
    assert parse_quarter("CHA LEADS SERIES 2-1 1st") == {"quarter": "1st"}


def test_parse_quarter_reads_overtime():
    assert parse_quarter("OT") == {"quarter": "OT"}


def test_parse_quarter_returns_none_when_no_period_is_found():
    assert parse_quarter("bonus") is None


def test_parse_shot_clock_reads_a_value_within_range():
    assert parse_shot_clock("14") == {"secondsRemaining": 14}
    assert parse_shot_clock(" 24 ") == {"secondsRemaining": 24}


def test_parse_shot_clock_rejects_a_value_above_twenty_four():
    assert parse_shot_clock("87") is None
