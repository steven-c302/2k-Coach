package com.nba2kassistant.core.roster.dto;

import com.nba2kassistant.core.player.PlayerResponse;

import java.util.List;

public record GeneratedRosterResponse(List<PlayerResponse> starters, List<PlayerResponse> bench) {
}
