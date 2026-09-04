package com.nba2kassistant.core.sync;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Map;

final class BadgeTier {

    private static final Map<String, Short> NAMED_TIERS = Map.of(
            "bronze", (short) 1,
            "silver", (short) 2,
            "gold", (short) 3,
            "hof", (short) 4,
            "hall of fame", (short) 4,
            "halloffame", (short) 4
    );

    private BadgeTier() {
    }

    static Short parse(JsonNode tierNode) {
        if (tierNode == null || tierNode.isNull()) {
            return null;
        }
        if (tierNode.isNumber()) {
            return tierNode.shortValue();
        }
        String key = tierNode.asText("").toLowerCase(Locale.ROOT).trim();
        return NAMED_TIERS.get(key);
    }
}
