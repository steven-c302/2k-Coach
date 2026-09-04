package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.nba2kassistant.core.roster.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;

class PositionCriterionTest {

    private final RosterBuildContext ctx = new RosterBuildContext(5, "CURRENT", null);

    @Test
    void keepsOnlyAllowedPositions() {
        List<Player> candidates = List.of(
                player(1, "Guard", "PG", 90),
                player(2, "Wing", "SF", 90),
                player(3, "Big", "C", 90)
        );

        List<Player> result = new PositionCriterion(Set.of("PG", "SF")).apply(candidates, ctx);

        assertThat(result).extracting(Player::getName).containsExactlyInAnyOrder("Guard", "Wing");
    }
}
