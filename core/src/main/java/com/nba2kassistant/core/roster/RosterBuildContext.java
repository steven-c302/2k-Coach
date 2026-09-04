package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

/** @param anchor the BUILD_AROUND player, if one was specified; null otherwise */
public record RosterBuildContext(int teamSize, String eraTag, Player anchor) {
}
