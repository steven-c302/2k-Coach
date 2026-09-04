package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

import java.util.List;

/** Ports nba2k-roster-randomizer's {@code filter_players_by_overall}. */
public final class OverallRangeCriterion implements RosterCriterion {

    private final int min;
    private final int max;

    public OverallRangeCriterion(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public List<Player> apply(List<Player> candidates, RosterBuildContext ctx) {
        return candidates.stream()
                .filter(p -> p.getOverall() >= min && p.getOverall() <= max)
                .toList();
    }
}
