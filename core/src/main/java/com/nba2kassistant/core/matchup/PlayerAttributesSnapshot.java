package com.nba2kassistant.core.matchup;

/** Detached copy of a player's attributes at analysis time — rules never touch JPA entities. */
public record PlayerAttributesSnapshot(
        Short threePt,
        Short midRange,
        Short layup,
        Short dunk,
        Short speed,
        Short strength,
        Short postDefense,
        Short perimeterDefense
) {
}
