from app.parsing import parse_clock, parse_score, parse_shot_clock


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


def test_parse_shot_clock_reads_a_value_within_range():
    assert parse_shot_clock("14") == {"secondsRemaining": 14}
    assert parse_shot_clock(" 24 ") == {"secondsRemaining": 24}


def test_parse_shot_clock_rejects_a_value_above_twenty_four():
    assert parse_shot_clock("87") is None
