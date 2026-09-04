package com.nba2kassistant.core.matchup;

import com.nba2kassistant.core.matchup.dto.MatchupAnalysisResponse;
import com.nba2kassistant.core.matchup.dto.MatchupAnalyzeRequest;
import com.nba2kassistant.core.matchup.dto.MismatchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchupAnalysisService {

    private final TeamSnapshotFactory snapshotFactory;
    private final List<MismatchRule> rules;

    public MatchupAnalysisService(TeamSnapshotFactory snapshotFactory, List<MismatchRule> rules) {
        this.snapshotFactory = snapshotFactory;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public MatchupAnalysisResponse analyze(MatchupAnalyzeRequest request) {
        TeamSnapshot teamA = snapshotFactory.build("Team A", request.teamAPlayerIds());
        TeamSnapshot teamB = snapshotFactory.build("Team B", request.teamBPlayerIds());

        List<MismatchResponse> mismatches = rules.stream()
                .flatMap(rule -> rule.evaluate(teamA, teamB).stream())
                .map(MismatchResponse::from)
                .toList();

        return new MatchupAnalysisResponse(mismatches);
    }
}
