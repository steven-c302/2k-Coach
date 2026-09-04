package com.nba2kassistant.core.roster.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record RosterGenerateRequest(
        List<RosterCriterionSpec> criteria,
        @Min(5) @Max(15) int teamSize,
        String era
) {
}
