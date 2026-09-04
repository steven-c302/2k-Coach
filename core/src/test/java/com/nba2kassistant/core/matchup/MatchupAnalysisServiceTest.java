package com.nba2kassistant.core.matchup;

import com.nba2kassistant.core.matchup.dto.MatchupAnalysisResponse;
import com.nba2kassistant.core.matchup.dto.MatchupAnalyzeRequest;
import com.nba2kassistant.core.matchup.dto.MismatchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.nba2kassistant.core.matchup.SnapshotFixtures.attributes;
import static com.nba2kassistant.core.matchup.SnapshotFixtures.player;
import static com.nba2kassistant.core.matchup.SnapshotFixtures.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Full-pipeline test: real rule instances, a stubbed snapshot factory boundary. */
@ExtendWith(MockitoExtension.class)
class MatchupAnalysisServiceTest {

    @Mock
    private TeamSnapshotFactory snapshotFactory;
    @Mock
    private MatchupHistoryRepository matchupHistoryRepository;

    @Test
    void combinesMismatchesFromEveryRule() {
        TeamSnapshot teamA = team("Team A",
                player(1, "Sharpshooter", "SG", attributes(96, 60, 60, 60, 60, 60, 60, 60)));
        TeamSnapshot teamB = team("Team B",
                player(2, "WeakDefender", "SG", attributes(60, 60, 60, 60, 60, 60, 60, 60)));

        when(snapshotFactory.build("Team A", List.of(1L))).thenReturn(teamA);
        when(snapshotFactory.build("Team B", List.of(2L))).thenReturn(teamB);

        MatchupAnalysisService service = new MatchupAnalysisService(
                snapshotFactory, List.of(new ThreePointVolumeRule(), new SpeedMismatchRule()), matchupHistoryRepository);

        MatchupAnalysisResponse response = service.analyze(new MatchupAnalyzeRequest(List.of(1L), List.of(2L)));

        assertThat(response.mismatches()).extracting(MismatchResponse::category).containsExactly("SHOOT_MORE_THREES");
    }

    @Test
    void returnsNoMismatchesWhenNoRuleFires() {
        TeamSnapshot teamA = team("Team A", player(1, "P1", "PG"));
        TeamSnapshot teamB = team("Team B", player(2, "P2", "PG"));

        when(snapshotFactory.build("Team A", List.of(1L))).thenReturn(teamA);
        when(snapshotFactory.build("Team B", List.of(2L))).thenReturn(teamB);

        MatchupAnalysisService service = new MatchupAnalysisService(
                snapshotFactory, List.of(new ThreePointVolumeRule()), matchupHistoryRepository);

        MatchupAnalysisResponse response = service.analyze(new MatchupAnalyzeRequest(List.of(1L), List.of(2L)));

        assertThat(response.mismatches()).isEmpty();
    }

    @Test
    void appendsEveryAnalysisToMatchupHistory() {
        TeamSnapshot teamA = team("Team A", player(1, "P1", "PG"));
        TeamSnapshot teamB = team("Team B", player(2, "P2", "PG"));
        when(snapshotFactory.build("Team A", List.of(1L))).thenReturn(teamA);
        when(snapshotFactory.build("Team B", List.of(2L))).thenReturn(teamB);

        MatchupAnalysisService service = new MatchupAnalysisService(
                snapshotFactory, List.of(), matchupHistoryRepository);

        service.analyze(new MatchupAnalyzeRequest(List.of(1L), List.of(2L)));

        verify(matchupHistoryRepository).append(any());
    }
}
