package com.nba2kassistant.core.coaching;

import com.nba2kassistant.core.matchup.dto.MismatchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateNarrationClientTest {

    private final TemplateNarrationClient client = new TemplateNarrationClient();

    private static NarrationContext contextWith(List<MismatchResponse> mismatches) {
        return new NarrationContext(mismatches, Map.of(), List.of(), List.of());
    }

    @Test
    void saysTheMatchupIsEvenWhenThereAreNoMismatches() {
        String narration = client.narrate(contextWith(List.of()));

        assertThat(narration).containsIgnoringCase("even");
    }

    @Test
    void leadsWithTheHighestSeverityMismatch() {
        MismatchResponse low = new MismatchResponse("DRIVE_MORE", "Team B", "LOW", "small gap");
        MismatchResponse high = new MismatchResponse("SHOOT_MORE_THREES", "Team A", "HIGH", "huge gap");

        String narration = client.narrate(contextWith(List.of(low, high)));

        assertThat(narration.indexOf("Team A")).isLessThan(narration.indexOf("Team B"));
    }

    @Test
    void humanizesTheCategoryName() {
        MismatchResponse mismatch = new MismatchResponse("SHOOT_MORE_THREES", "Team A", "HIGH", "evidence");

        String narration = client.narrate(contextWith(List.of(mismatch)));

        assertThat(narration).contains("shoot more threes");
    }

    @Test
    void appendsLiveGameStateWhenPresent() {
        NarrationContext context = new NarrationContext(List.of(), Map.of("teamAScore", 52, "teamBScore", 60), List.of(), List.of());

        String narration = client.narrate(context);

        assertThat(narration).contains("teamAScore=52").contains("teamBScore=60");
    }
}
