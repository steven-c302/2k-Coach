package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nba2kassistant.core.roster.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;

class EraCriterionTest {

    private final RosterBuildContext ctx = new RosterBuildContext(5, "CURRENT", null);

    @Test
    void keepsOnlyMatchingEraTag() {
        List<Player> candidates = List.of(
                player(1, "Modern", "PG", 90, "CURRENT"),
                player(2, "Classic96", "PG", 99, "1996"),
                player(3, "Classic13", "PG", 97, "2013")
        );

        List<Player> result = new EraCriterion("1996").apply(candidates, ctx);

        assertThat(result).extracting(Player::getName).containsExactly("Classic96");
    }
}
