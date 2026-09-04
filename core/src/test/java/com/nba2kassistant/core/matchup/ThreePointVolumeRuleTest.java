package com.nba2kassistant.core.matchup;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nba2kassistant.core.matchup.SnapshotFixtures.attributes;
import static com.nba2kassistant.core.matchup.SnapshotFixtures.player;
import static com.nba2kassistant.core.matchup.SnapshotFixtures.team;
import static org.assertj.core.api.Assertions.assertThat;

class ThreePointVolumeRuleTest {

    private final ThreePointVolumeRule rule = new ThreePointVolumeRule();

    @Test
    void flagsShootMoreThreesWhenGapExceedsThreshold() {
        TeamSnapshot teamA = team("Team A",
                player(1, "Sharpshooter", "SG", attributes(95, 80, 80, 80, 80, 80, 80, 80)));
        TeamSnapshot teamB = team("Team B",
                player(2, "WeakDefender", "SG", attributes(80, 80, 80, 80, 80, 80, 80, 70)));

        List<Mismatch> result = rule.evaluate(teamA, teamB);

        assertThat(result).hasSize(1);
        Mismatch mismatch = result.get(0);
        assertThat(mismatch.category()).isEqualTo("SHOOT_MORE_THREES");
        assertThat(mismatch.favoredTeam()).isEqualTo("Team A");
        assertThat(mismatch.severity()).isEqualTo("HIGH"); // 95 - 70 = 25pt gap
    }

    @Test
    void staysSilentWhenGapIsBelowThreshold() {
        TeamSnapshot teamA = team("Team A",
                player(1, "P1", "SG", attributes(85, 80, 80, 80, 80, 80, 80, 80)));
        TeamSnapshot teamB = team("Team B",
                player(2, "P2", "SG", attributes(80, 80, 80, 80, 80, 80, 80, 80))); // only a 5pt gap

        assertThat(rule.evaluate(teamA, teamB)).isEmpty();
    }

    @Test
    void staysSilentWhenNeitherTeamHasAttributeData() {
        TeamSnapshot teamA = team("Team A", player(1, "P1", "SG"));
        TeamSnapshot teamB = team("Team B", player(2, "P2", "SG"));

        assertThat(rule.evaluate(teamA, teamB)).isEmpty();
    }

    @Test
    void canFlagBothDirectionsIndependently() {
        TeamSnapshot teamA = team("Team A",
                player(1, "A_Shooter", "SG", attributes(95, 80, 80, 80, 80, 80, 80, 60)));
        TeamSnapshot teamB = team("Team B",
                player(2, "B_Shooter", "SG", attributes(95, 80, 80, 80, 80, 80, 80, 60)));

        List<Mismatch> result = rule.evaluate(teamA, teamB);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Mismatch::favoredTeam).containsExactlyInAnyOrder("Team A", "Team B");
    }
}
