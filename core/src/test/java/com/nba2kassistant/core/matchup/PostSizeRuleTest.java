package com.nba2kassistant.core.matchup;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nba2kassistant.core.matchup.SnapshotFixtures.attributes;
import static com.nba2kassistant.core.matchup.SnapshotFixtures.player;
import static com.nba2kassistant.core.matchup.SnapshotFixtures.team;
import static org.assertj.core.api.Assertions.assertThat;

class PostSizeRuleTest {

    private final PostSizeRule rule = new PostSizeRule();

    @Test
    void flagsAttackInsideWhenBigsOutmuscleOpposingPostDefense() {
        TeamSnapshot teamA = team("Team A",
                player(1, "BigMan", "C", attributes(60, 60, 60, 60, 60, 95, 60, 60)));
        TeamSnapshot teamB = team("Team B",
                player(2, "WeakBig", "C", attributes(60, 60, 60, 60, 60, 60, 70, 60)));

        List<Mismatch> result = rule.evaluate(teamA, teamB);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("ATTACK_INSIDE");
        assertThat(result.get(0).favoredTeam()).isEqualTo("Team A");
    }

    @Test
    void ignoresNonPostPlayersWhenComparing() {
        TeamSnapshot teamA = team("Team A",
                player(1, "Guard", "PG", attributes(60, 60, 60, 60, 60, 99, 60, 60))); // strong but not a post player
        TeamSnapshot teamB = team("Team B",
                player(2, "WeakBig", "C", attributes(60, 60, 60, 60, 60, 60, 70, 60)));

        // Team A has no post players at all, so the rule can't judge it — must stay silent.
        assertThat(rule.evaluate(teamA, teamB)).isEmpty();
    }

    @Test
    void staysSilentWhenGapIsBelowThreshold() {
        TeamSnapshot teamA = team("Team A",
                player(1, "BigMan", "C", attributes(60, 60, 60, 60, 60, 78, 60, 60)));
        TeamSnapshot teamB = team("Team B",
                player(2, "SolidBig", "C", attributes(60, 60, 60, 60, 60, 60, 75, 60))); // 3pt gap only

        assertThat(rule.evaluate(teamA, teamB)).isEmpty();
    }
}
