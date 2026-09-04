package com.nba2kassistant.core.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Mirrors the nba2kapi player shape from its README (name/slug/team/overall/
 * positions/attributes/badges). {@code attributes} and {@code badges} are kept
 * as raw {@link JsonNode} rather than a strict DTO: the README only shows a
 * partial example ("shooting" category only) so the full set of nested field
 * names is unconfirmed until this runs against a live key — see
 * {@link AttributeExtractor}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NbaTwoKApiPlayerDto(
        String name,
        String slug,
        String team,
        Integer overall,
        List<String> positions,
        JsonNode attributes,
        JsonNode badges
) {
}
