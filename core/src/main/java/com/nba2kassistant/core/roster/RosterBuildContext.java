package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

/**
 * @param anchor the BUILD_AROUND player, if one was specified; null otherwise
 * @param aboveCount/belowCount how many players (starters + bench combined) must meet the
 *        matching threshold; 0 disables that half of the quota
 * @param targetAverage OVERALL_AVERAGE target for the whole roster; 0 disables it
 */
public record RosterBuildContext(
        int teamSize,
        String eraTag,
        Player anchor,
        int aboveThreshold,
        int aboveCount,
        int belowThreshold,
        int belowCount,
        int targetAverage
) {

    public RosterBuildContext(int teamSize, String eraTag, Player anchor) {
        this(teamSize, eraTag, anchor, 0, 0, 0, 0, 0);
    }
}
