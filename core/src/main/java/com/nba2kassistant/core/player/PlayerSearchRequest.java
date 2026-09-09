package com.nba2kassistant.core.player;

public record PlayerSearchRequest(
        String position,
        Integer minOverall,
        Integer maxOverall,
        String eraTag,
        String team,
        String name,
        Integer minThreePt,
        Integer minMidRange,
        Integer minLayup,
        Integer minDunk,
        Integer minSpeed,
        Integer minStrength,
        Integer minPostDefense,
        Integer minPerimeterDefense
) {
}
