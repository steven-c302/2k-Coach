package com.nba2kassistant.core.matchup;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nba2kassistant.core.matchup.SnapshotFixtures.attributes;
import static com.nba2kassistant.core.matchup.SnapshotFixtures.player;
import static com.nba2kassistant.core.matchup.SnapshotFixtures.team;
import static org.assertj.core.api.Assertions.assertThat;

class SpeedMismatchRuleTest {

    private final SpeedMismatchRule rule = new SpeedMismatchRule();

    @Test
    void flagsDriveMoreForTheFasterTeam() {
        TeamSnapshot teamA = team("Team A",
                player(1, "Speedster", "PG", attributes(60, 60, 60, 60, 96, 60, 60, 60)));
        TeamSnapshot teamB = team("Team B",
                player(2, "Plodder", "PG", attributes(60, 60, 60, 60, 70, 60, 60, 60)));

        List<Mismatch> result = rule.evaluate(teamA, teamB);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("DRIVE_MORE");
        assertThat(result.get(0).favoredTeam()).isEqualTo("Team A");
    }

    @Test
    void staysSilentWhenSpeedsAreClose() {
        TeamSnapshot teamA = team("Team A", player(1, "P1", "PG", attributes(60, 60, 60, 60, 85, 60, 60, 60)));
        TeamSnapshot teamB = team("Team B", player(2, "P2", "PG", attributes(60, 60, 60, 60, 82, 60, 60, 60)));

        assertThat(rule.evaluate(teamA, teamB)).isEmpty();
    }
}
