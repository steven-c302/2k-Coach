package com.nba2kassistant.core.matchup.dto;

import java.util.List;

public record MatchupAnalysisResponse(List<MismatchResponse> mismatches) {
}
