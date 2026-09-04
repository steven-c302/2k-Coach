package com.nba2kassistant.core.session.dto;

import java.util.List;

public record AnalyzeMatchupMessage(List<Long> teamAPlayerIds, List<Long> teamBPlayerIds) {
}
