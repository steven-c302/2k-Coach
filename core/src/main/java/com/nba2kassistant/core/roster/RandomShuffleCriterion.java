package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default terminal pipeline step when no BUILD_AROUND criterion is present.
 * Without this, RosterFillService would always take the same players (DB
 * order) for a given filter set — nba2k-roster-randomizer's whole point was
 * {@code random.sample}-based variety, so that's preserved here rather than
 * dropped when porting to a deterministic query pipeline.
 */
public final class RandomShuffleCriterion implements RosterCriterion {

    @Override
    public List<Player> apply(List<Player> candidates, RosterBuildContext ctx) {
        List<Player> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        return shuffled;
    }
}
