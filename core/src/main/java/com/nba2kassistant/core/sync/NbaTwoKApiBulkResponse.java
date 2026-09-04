package com.nba2kassistant.core.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NbaTwoKApiBulkResponse(boolean success, List<NbaTwoKApiPlayerDto> data) {
}
