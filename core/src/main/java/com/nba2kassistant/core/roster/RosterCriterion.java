package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

import java.util.List;

/**
 * One filtering/ordering step in the roster-generation pipeline. Generalizes
 * nba2k-roster-randomizer's two hardcoded filter functions into a composable
 * strategy (NBA2K Assistant plan §3) — each implementation is pure and
 * independently testable against fixed {@link Player} fixtures, no mocks.
 */
public interface RosterCriterion {

    List<Player> apply(List<Player> candidates, RosterBuildContext ctx);
}
