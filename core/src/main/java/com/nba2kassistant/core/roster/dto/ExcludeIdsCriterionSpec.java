package com.nba2kassistant.core.roster.dto;

import java.util.List;

public record ExcludeIdsCriterionSpec(List<Long> ids) implements RosterCriterionSpec {
}
