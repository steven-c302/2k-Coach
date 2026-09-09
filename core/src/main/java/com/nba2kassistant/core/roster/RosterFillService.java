package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Ports nba2k-roster-randomizer's {@code create_rosters_with_starters} "best
 * overall per position starts" rule for a single roster (the original
 * distributed rosters across multiple users; this generalizes to the
 * team-builder's one-roster-per-request shape).
 *
 * <p>Algorithm: fill the 5 required positions plus bench spots from the
 * pipeline's (already filtered/ordered) candidate list, taking the
 * highest-ranked remaining candidate at each step — then recompute the
 * actual starters as the best-overall player at each position across the
 * WHOLE assembled roster, exactly like the original: a bench pickup can
 * still outrank whoever was initially assigned to that position. A
 * BUILD_AROUND anchor is the one exception — they're pinned as their
 * position's starter unconditionally, since guaranteeing the anchor a
 * starting spot is the entire point of that criterion.
 */
@Component
public class RosterFillService {

    private static final List<String> REQUIRED_POSITIONS = List.of("PG", "SG", "SF", "PF", "C");

    public GeneratedRoster fill(List<Player> orderedCandidates, RosterBuildContext ctx) {
        if (ctx.teamSize() < 5 || ctx.teamSize() > 15) {
            throw new RosterGenerationException("teamSize must be between 5 and 15");
        }

        List<Player> pool = new ArrayList<>(orderedCandidates);
        List<Player> roster = new ArrayList<>();
        Player anchor = ctx.anchor();

        Set<String> positionsToFill = new LinkedHashSet<>(REQUIRED_POSITIONS);
        if (anchor != null) {
            roster.add(anchor);
            positionsToFill.remove(anchor.getPosition());
            pool.removeIf(p -> Objects.equals(p.getId(), anchor.getId()));
        }

        for (String position : positionsToFill) {
            Player pick = pool.stream()
                    .filter(p -> position.equals(p.getPosition()))
                    .findFirst()
                    .orElseThrow(() -> new RosterGenerationException("Not enough players available at position " + position));
            roster.add(pick);
            pool.remove(pick);
        }

        int benchSpotsNeeded = ctx.teamSize() - roster.size();
        if (pool.size() < benchSpotsNeeded) {
            throw new RosterGenerationException("Not enough players available for bench spots");
        }
        fillBench(pool, roster, ctx, benchSpotsNeeded);

        List<Player> starters = new ArrayList<>();
        for (String position : REQUIRED_POSITIONS) {
            if (anchor != null && position.equals(anchor.getPosition())) {
                starters.add(anchor);
                continue;
            }
            Player best = roster.stream()
                    .filter(p -> position.equals(p.getPosition()))
                    .max(Comparator.comparing(Player::getOverall))
                    .orElseThrow(() -> new RosterGenerationException("No player found for starting position " + position));
            starters.add(best);
        }

        List<Player> bench = roster.stream()
                .filter(p -> !starters.contains(p))
                .sorted(Comparator.comparing(Player::getOverall).reversed())
                .toList();

        return new GeneratedRoster(starters, bench);
    }

    /**
     * Fills bench spots one at a time, cycling the 5 positions in blocks so a block never repeats
     * a position until every position has appeared once (e.g. on a 10-man roster, bench spots 6-10
     * — one full block — each land on a different position, same as the 5 starters do). Within
     * that constraint, an unmet OVERALL_DISTRIBUTION quota (see {@link RosterBuildContext}) takes
     * priority over the pool's default ordering; if no candidate satisfies both, priorities are
     * relaxed one at a time (quota first, then position) rather than failing the whole roster.
     */
    private void fillBench(List<Player> pool, List<Player> roster, RosterBuildContext ctx, int benchSpotsNeeded) {
        int aboveRemaining = Math.max(0, ctx.aboveCount()
                - (int) roster.stream().filter(p -> p.getOverall() >= ctx.aboveThreshold()).count());
        int belowRemaining = Math.max(0, ctx.belowCount()
                - (int) roster.stream().filter(p -> p.getOverall() <= ctx.belowThreshold()).count());

        Set<String> usedInBlock = new LinkedHashSet<>();
        for (int i = 0; i < benchSpotsNeeded; i++) {
            if (usedInBlock.size() >= REQUIRED_POSITIONS.size()) {
                usedInBlock.clear();
            }
            Set<String> allowedPositions = new LinkedHashSet<>(REQUIRED_POSITIONS);
            allowedPositions.removeAll(usedInBlock);

            Player pick = null;
            if (aboveRemaining > 0) {
                pick = firstMatch(pool, p -> allowedPositions.contains(p.getPosition()) && p.getOverall() >= ctx.aboveThreshold());
                if (pick == null) {
                    pick = firstMatch(pool, p -> p.getOverall() >= ctx.aboveThreshold());
                }
                if (pick != null) {
                    aboveRemaining--;
                }
            }
            if (pick == null && belowRemaining > 0) {
                pick = firstMatch(pool, p -> allowedPositions.contains(p.getPosition()) && p.getOverall() <= ctx.belowThreshold());
                if (pick == null) {
                    pick = firstMatch(pool, p -> p.getOverall() <= ctx.belowThreshold());
                }
                if (pick != null) {
                    belowRemaining--;
                }
            }
            if (pick == null) {
                pick = firstMatch(pool, p -> allowedPositions.contains(p.getPosition()));
            }
            if (pick == null) {
                pick = pool.get(0);
            }

            roster.add(pick);
            pool.remove(pick);
            usedInBlock.add(pick.getPosition());
        }
    }

    private Player firstMatch(List<Player> pool, java.util.function.Predicate<Player> predicate) {
        return pool.stream().filter(predicate).findFirst().orElse(null);
    }
}
