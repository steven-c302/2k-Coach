package com.nba2kassistant.core.player;

public record PlayerSearchRequest(
        String position,
        Integer minOverall,
        Integer maxOverall,
        String eraTag,
        String name
) {
}
