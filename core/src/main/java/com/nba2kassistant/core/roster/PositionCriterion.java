package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

import java.util.List;
import java.util.Set;

/** Ports nba2k-roster-randomizer's {@code filter_players_by_position}. */
public final class PositionCriterion implements RosterCriterion {

    private final Set<String> allowedPositions;

    public PositionCriterion(Set<String> allowedPositions) {
        this.allowedPositions = allowedPositions;
    }

    @Override
    public List<Player> apply(List<Player> candidates, RosterBuildContext ctx) {
        return candidates.stream()
                .filter(p -> allowedPositions.contains(p.getPosition()))
                .toList();
    }
}
