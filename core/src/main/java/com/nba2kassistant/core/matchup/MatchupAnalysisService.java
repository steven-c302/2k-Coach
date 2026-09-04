package com.nba2kassistant.core.matchup;

import com.nba2kassistant.core.matchup.dto.MatchupAnalysisResponse;
import com.nba2kassistant.core.matchup.dto.MatchupAnalyzeRequest;
import com.nba2kassistant.core.matchup.dto.MismatchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchupAnalysisService {

    private final TeamSnapshotFactory snapshotFactory;
    private final List<MismatchRule> rules;
    private final MatchupHistoryRepository matchupHistoryRepository;

    public MatchupAnalysisService(
            TeamSnapshotFactory snapshotFactory,
            List<MismatchRule> rules,
            MatchupHistoryRepository matchupHistoryRepository
    ) {
        this.snapshotFactory = snapshotFactory;
        this.rules = rules;
        this.matchupHistoryRepository = matchupHistoryRepository;
    }

    @Transactional(readOnly = true)
    public MatchupAnalysisResponse analyze(MatchupAnalyzeRequest request) {
        TeamSnapshot teamA = snapshotFactory.build("Team A", request.teamAPlayerIds());
        TeamSnapshot teamB = snapshotFactory.build("Team B", request.teamBPlayerIds());

        List<MismatchResponse> mismatches = rules.stream()
                .flatMap(rule -> rule.evaluate(teamA, teamB).stream())
                .map(MismatchResponse::from)
                .toList();

        appendToHistory(request, mismatches);

        return new MatchupAnalysisResponse(mismatches);
    }

    private void appendToHistory(MatchupAnalyzeRequest request, List<MismatchResponse> mismatches) {
        String sessionCode = request.sessionCode() == null ? MatchupHistoryRepository.NO_SESSION : request.sessionCode();
        String mismatchSummary = mismatches.isEmpty()
                ? "none"
                : mismatches.stream().map(m -> m.category() + ":" + m.favoredTeam()).collect(Collectors.joining(", "));

        matchupHistoryRepository.append(MatchupHistoryEntry.of(
                sessionCode,
                Instant.now() + "#MATCHUP_ANALYZED",
                joinIds(request.teamAPlayerIds()),
                joinIds(request.teamBPlayerIds()),
                mismatchSummary
        ));
    }

    private static String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
