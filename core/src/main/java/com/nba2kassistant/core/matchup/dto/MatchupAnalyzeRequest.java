package com.nba2kassistant.core.matchup.dto;

import java.util.List;

/** @param sessionCode null for a bare API call — MatchupHistory uses "STANDALONE" for those (see MatchupHistoryRepository). */
public record MatchupAnalyzeRequest(List<Long> teamAPlayerIds, List<Long> teamBPlayerIds, String sessionCode) {

    public MatchupAnalyzeRequest(List<Long> teamAPlayerIds, List<Long> teamBPlayerIds) {
        this(teamAPlayerIds, teamBPlayerIds, null);
    }
}
