package com.nba2kassistant.core.coaching;

import com.nba2kassistant.core.matchup.dto.MismatchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateNarrationClientTest {

    private final TemplateNarrationClient client = new TemplateNarrationClient();

    @Test
    void saysTheMatchupIsEvenWhenThereAreNoMismatches() {
        String narration = client.narrate(List.of());

        assertThat(narration).containsIgnoringCase("even");
    }

    @Test
    void leadsWithTheHighestSeverityMismatch() {
        MismatchResponse low = new MismatchResponse("DRIVE_MORE", "Team B", "LOW", "small gap");
        MismatchResponse high = new MismatchResponse("SHOOT_MORE_THREES", "Team A", "HIGH", "huge gap");

        String narration = client.narrate(List.of(low, high));

        assertThat(narration.indexOf("Team A")).isLessThan(narration.indexOf("Team B"));
    }

    @Test
    void humanizesTheCategoryName() {
        MismatchResponse mismatch = new MismatchResponse("SHOOT_MORE_THREES", "Team A", "HIGH", "evidence");

        String narration = client.narrate(List.of(mismatch));

        assertThat(narration).contains("shoot more threes");
    }
}
