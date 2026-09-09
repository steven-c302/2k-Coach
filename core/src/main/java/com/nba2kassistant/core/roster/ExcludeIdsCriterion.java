package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

import java.util.List;
import java.util.Set;

/** Keeps a second generated roster (e.g. "Team B") from reusing a player already on Team A. */
public final class ExcludeIdsCriterion implements RosterCriterion {

    private final Set<Long> excludedIds;

    public ExcludeIdsCriterion(Set<Long> excludedIds) {
        this.excludedIds = excludedIds;
    }

    @Override
    public List<Player> apply(List<Player> candidates, RosterBuildContext ctx) {
        return candidates.stream()
                .filter(p -> !excludedIds.contains(p.getId()))
                .toList();
    }
}
