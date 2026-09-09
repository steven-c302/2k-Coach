package com.nba2kassistant.core.roster.dto;

/**
 * Not a filter — a fill-time target read by {@link com.nba2kassistant.core.roster.RosterFillService}:
 * steer picks so the whole roster's overall averages close to {@code target}, allowing individual
 * players well outside any OVERALL_RANGE (e.g. two 60s and a 90 to average 70).
 */
public record OverallAverageCriterionSpec(int target) implements RosterCriterionSpec {
}
