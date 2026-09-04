package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * New in the Java port (not in nba2k-roster-randomizer) — plan §3. Ranks
 * candidates by how well they complement a fixed anchor player: a different
 * position scores higher (the anchor already covers theirs), and attributes
 * where the candidate is strong exactly where the anchor is weak score
 * higher still (see AttributeNeed). The anchor is excluded from the returned
 * list — RosterFillService pins them into the roster directly rather than
 * re-selecting them from the pool.
 *
 * <p>This is a self-contained heuristic, not shared with the coaching rules
 * engine (Milestone 4, not yet built) despite doing conceptually similar
 * "where does this team need help" scoring — if Milestone 4 duplicates this
 * logic, that's the point to extract a shared attribute-comparison utility,
 * not before.
 */
public final class BuildAroundPlayerCriterion implements RosterCriterion {

    private static final double DIFFERENT_POSITION_BONUS = 10;

    private final Player anchor;

    public BuildAroundPlayerCriterion(Player anchor) {
        this.anchor = anchor;
    }

    @Override
    public List<Player> apply(List<Player> candidates, RosterBuildContext ctx) {
        return candidates.stream()
                .filter(p -> !Objects.equals(p.getId(), anchor.getId()))
                .sorted(Comparator.comparingDouble(this::complementScore).reversed())
                .toList();
    }

    private double complementScore(Player candidate) {
        double score = Objects.equals(candidate.getPosition(), anchor.getPosition()) ? 0 : DIFFERENT_POSITION_BONUS;
        return score + AttributeNeed.complementScore(anchor.getAttributes(), candidate.getAttributes());
    }
}
