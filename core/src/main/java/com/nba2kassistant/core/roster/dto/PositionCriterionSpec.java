package com.nba2kassistant.core.roster.dto;

import java.util.List;

public record PositionCriterionSpec(List<String> allowed) implements RosterCriterionSpec {
}
