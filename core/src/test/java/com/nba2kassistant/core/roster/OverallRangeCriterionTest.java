package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nba2kassistant.core.roster.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;

class OverallRangeCriterionTest {

    private final RosterBuildContext ctx = new RosterBuildContext(5, "CURRENT", null);

    @Test
    void keepsOnlyPlayersWithinRangeInclusive() {
        List<Player> candidates = List.of(
                player(1, "Below", "PG", 79),
                player(2, "AtMin", "PG", 80),
                player(3, "Middle", "PG", 90),
                player(4, "AtMax", "PG", 99),
                player(5, "Above", "PG", 100)
        );

        List<Player> result = new OverallRangeCriterion(80, 99).apply(candidates, ctx);

        assertThat(result).extracting(Player::getName).containsExactly("AtMin", "Middle", "AtMax");
    }
}
