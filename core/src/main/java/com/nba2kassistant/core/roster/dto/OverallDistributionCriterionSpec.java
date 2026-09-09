package com.nba2kassistant.core.roster.dto;

/**
 * Not a filter — a fill-time quota read by {@link com.nba2kassistant.core.roster.RosterFillService}:
 * "at least {@code aboveCount} players with overall >= {@code aboveThreshold}" and/or
 * "at least {@code belowCount} players with overall <= {@code belowThreshold}", counted across
 * the whole roster (starters + bench). A null/zero count disables that half of the quota.
 */
public record OverallDistributionCriterionSpec(
        Integer aboveThreshold,
        Integer aboveCount,
        Integer belowThreshold,
        Integer belowCount
) implements RosterCriterionSpec {
}
