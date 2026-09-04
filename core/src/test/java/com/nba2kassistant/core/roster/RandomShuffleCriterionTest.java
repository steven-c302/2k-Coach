package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nba2kassistant.core.roster.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;

class RandomShuffleCriterionTest {

    private final RosterBuildContext ctx = new RosterBuildContext(5, "CURRENT", null);

    @Test
    void preservesAllCandidatesWithoutDuplicationOrLoss() {
        List<Player> candidates = List.of(
                player(1, "A", "PG", 80),
                player(2, "B", "SG", 80),
                player(3, "C", "SF", 80)
        );

        List<Player> result = new RandomShuffleCriterion().apply(candidates, ctx);

        assertThat(result).containsExactlyInAnyOrderElementsOf(candidates);
    }

    @Test
    void doesNotMutateTheInputList() {
        List<Player> candidates = List.of(player(1, "A", "PG", 80));

        new RandomShuffleCriterion().apply(candidates, ctx);

        assertThat(candidates).hasSize(1); // would throw on the immutable List.of() if mutated in place
    }
}
