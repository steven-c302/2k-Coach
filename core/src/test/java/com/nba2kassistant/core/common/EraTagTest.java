package com.nba2kassistant.core.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EraTagTest {

    static Stream<Arguments> classicTeamEncodings() {
        return Stream.of(
                Arguments.of("'96 CHI", "CHI", "1996"),
                Arguments.of("'13 MIA", "MIA", "2013"),
                Arguments.of("'19 TOR", "TOR", "2019"),
                Arguments.of("'01 PHI", "PHI", "2001"),
                Arguments.of("'26 BOS", "BOS", "2026")
        );
    }

    @ParameterizedTest
    @MethodSource("classicTeamEncodings")
    void parsesClassicTeamEncodingAgainstA26Pivot(String raw, String expectedTeam, String expectedEra) {
        EraTag.Parsed parsed = EraTag.parse(raw, 26);

        assertThat(parsed.team()).isEqualTo(expectedTeam);
        assertThat(parsed.eraTag()).isEqualTo(expectedEra);
    }

    @Test
    void twoDigitYearAboveThePivotResolvesToNineteenHundreds() {
        EraTag.Parsed parsed = EraTag.parse("'89 DET", 26);

        assertThat(parsed.eraTag()).isEqualTo("1989");
    }

    @Test
    void fullTeamNameIsTreatedAsCurrentRoster() {
        EraTag.Parsed parsed = EraTag.parse("Golden State Warriors", 26);

        assertThat(parsed.team()).isEqualTo("Golden State Warriors");
        assertThat(parsed.eraTag()).isEqualTo(EraTag.CURRENT);
    }

    @Test
    void nullTeamIsTreatedAsCurrentRoster() {
        EraTag.Parsed parsed = EraTag.parse(null, 26);

        assertThat(parsed.team()).isNull();
        assertThat(parsed.eraTag()).isEqualTo(EraTag.CURRENT);
    }
}
