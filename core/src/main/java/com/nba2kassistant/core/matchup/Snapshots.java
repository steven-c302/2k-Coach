package com.nba2kassistant.core.matchup;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 * Attribute averaging that skips missing values instead of treating them as
 * 0 — most seeded players (local-scraper fallback, no nba2kapi sync) have no
 * attributes at all, and a false "0 vs real value" gap would produce
 * nonsense mismatches. Returns empty only when NO player in the group has
 * that attribute; rules treat empty as "can't judge this, stay silent."
 */
final class Snapshots {

    private Snapshots() {
    }

    static OptionalDouble average(List<PlayerSnapshot> players, Function<PlayerAttributesSnapshot, Short> extractor) {
        return players.stream()
                .map(PlayerSnapshot::attributes)
                .filter(Objects::nonNull)
                .map(extractor)
                .filter(Objects::nonNull)
                .mapToInt(Short::intValue)
                .average();
    }
}
