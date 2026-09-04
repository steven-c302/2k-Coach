package com.nba2kassistant.core.matchup.dto;

import java.util.List;

public record MatchupAnalyzeRequest(List<Long> teamAPlayerIds, List<Long> teamBPlayerIds) {
}
