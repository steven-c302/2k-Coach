package com.nba2kassistant.core.matchup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeverityTest {

    @Test
    void bucketsByMagnitudeRegardlessOfSign() {
        assertThat(Severity.of(16)).isEqualTo("HIGH");
        assertThat(Severity.of(-16)).isEqualTo("HIGH");
        assertThat(Severity.of(12)).isEqualTo("MEDIUM");
        assertThat(Severity.of(9)).isEqualTo("LOW");
    }
}
