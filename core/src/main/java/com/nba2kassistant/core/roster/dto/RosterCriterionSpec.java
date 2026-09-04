package com.nba2kassistant.core.roster.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Wire-format criteria for {@code POST /api/rosters/generate}, dispatched by "type". */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OverallRangeCriterionSpec.class, name = "OVERALL_RANGE"),
        @JsonSubTypes.Type(value = PositionCriterionSpec.class, name = "POSITION"),
        @JsonSubTypes.Type(value = BuildAroundCriterionSpec.class, name = "BUILD_AROUND")
})
public sealed interface RosterCriterionSpec
        permits OverallRangeCriterionSpec, PositionCriterionSpec, BuildAroundCriterionSpec {
}
