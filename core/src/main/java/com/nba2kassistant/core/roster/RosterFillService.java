package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Ports nba2k-roster-randomizer's {@code create_rosters_with_starters} "best
 * overall per position starts" rule for a single roster (the original
 * distributed rosters across multiple users; this generalizes to the
 * team-builder's one-roster-per-request shape).
 *
 * <p>Algorithm: fill the 5 required positions plus bench spots, taking the
 * highest-ranked remaining candidate at each step — then recompute the
 * actual starters as the best-overall player at each position across the
 * WHOLE assembled roster, exactly like the original: a bench pickup can
 * still outrank whoever was initially assigned to that position. A
 * BUILD_AROUND anchor is the one exception — they're pinned as their
 * position's starter unconditionally, since guaranteeing the anchor a
 * starting spot is the entire point of that criterion.
 *
 * <p>Every slot (starters included, not just bench) is chosen through one
 * shared {@link #pickForSlot} priority order: an unmet OVERALL_DISTRIBUTION
 * quota first, then an active OVERALL_AVERAGE target, then the plain
 * range-respecting default. The first two deliberately search {@code
 * unrestricted} — the same candidate pool minus any OVERALL_RANGE filter —
 * because both features are pointless if they can't reach outside a range
 * the caller also set (e.g. "2 players >= 90 overall" needs somewhere to
 * find a 90 even if the stated range tops out at 80).
 */
@Component
public class RosterFillService {

    private static final List<String> REQUIRED_POSITIONS = List.of("PG", "SG", "SF", "PF", "C");

    public GeneratedRoster fill(List<Player> preferredCandidates, List<Player> unrestrictedCandidates, RosterBuildContext ctx) {
        if (ctx.teamSize() < 5 || ctx.teamSize() > 15) {
            throw new RosterGenerationException("teamSize must be between 5 and 15");
        }

        List<Player> preferred = new ArrayList<>(preferredCandidates);
        List<Player> unrestricted = new ArrayList<>(unrestrictedCandidates);
        List<Player> roster = new ArrayList<>();
        Player anchor = ctx.anchor();

        QuotaState quota = new QuotaState(ctx);
        AverageState average = new AverageState(ctx);

        Set<String> positionsToFill = new LinkedHashSet<>(REQUIRED_POSITIONS);
        if (anchor != null) {
            roster.add(anchor);
            positionsToFill.remove(anchor.getPosition());
            remove(preferred, unrestricted, anchor);
            quota.record(anchor);
            average.record(anchor);
        }

        for (String position : positionsToFill) {
            // false: a required starting position must actually be filled by that position - never
            // silently substitute a different position, that would just surface as a confusing
            // "no player found for starting position X" from the recomputation step below instead
            // of this clear one.
            Player pick = pickForSlot(preferred, unrestricted, Set.of(position), quota, average, false);
            if (pick == null) {
                throw new RosterGenerationException("Not enough players available at position " + position);
            }
            roster.add(pick);
            remove(preferred, unrestricted, pick);
        }

        int benchSpotsNeeded = ctx.teamSize() - roster.size();
        fillBench(preferred, unrestricted, roster, quota, average, benchSpotsNeeded);

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
     * — one full block — each land on a different position, same as the 5 starters do).
     */
    private void fillBench(List<Player> preferred, List<Player> unrestricted, List<Player> roster,
                            QuotaState quota, AverageState average, int benchSpotsNeeded) {
        Set<String> usedInBlock = new LinkedHashSet<>();
        for (int i = 0; i < benchSpotsNeeded; i++) {
            if (usedInBlock.size() >= REQUIRED_POSITIONS.size()) {
                usedInBlock.clear();
            }
            Set<String> allowedPositions = new LinkedHashSet<>(REQUIRED_POSITIONS);
            allowedPositions.removeAll(usedInBlock);

            // true: bench position diversity is a soft preference - fall back to any position
            // rather than fail the whole roster when the pool can't keep the block fully distinct.
            Player pick = pickForSlot(preferred, unrestricted, allowedPositions, quota, average, true);
            if (pick == null) {
                throw new RosterGenerationException("Not enough players available for bench spots");
            }

            roster.add(pick);
            remove(preferred, unrestricted, pick);
            usedInBlock.add(pick.getPosition());
        }
    }

    private Player pickForSlot(List<Player> preferred, List<Player> unrestricted, Set<String> allowedPositions,
                                QuotaState quota, AverageState average, boolean allowAnyPositionFallback) {
        // quota.record() below is the single source of truth for decrementing both counters - it
        // fires for every pick, quota-seeking or not, so a naturally-qualifying default/average
        // pick still counts. The seeking branches here only choose WHICH player, never decrement.
        Player pick = null;

        if (quota.aboveRemaining > 0) {
            pick = quotaMatch(preferred, unrestricted, allowedPositions, p -> p.getOverall() >= quota.aboveThreshold);
        }
        if (pick == null && quota.belowRemaining > 0) {
            pick = quotaMatch(preferred, unrestricted, allowedPositions, p -> p.getOverall() <= quota.belowThreshold);
        }
        if (pick == null && average.active) {
            double ideal = average.idealNext();
            pick = closestByOverall(unrestricted, allowedPositions, ideal);
            if (pick == null) {
                pick = closestByOverall(unrestricted, null, ideal);
            }
        }
        if (pick == null) {
            pick = firstMatch(preferred, p -> allowedPositions.contains(p.getPosition()));
        }
        if (pick == null) {
            pick = firstMatch(unrestricted, p -> allowedPositions.contains(p.getPosition()));
        }
        if (pick == null && allowAnyPositionFallback) {
            pick = firstMatch(preferred, p -> true);
        }
        if (pick == null && allowAnyPositionFallback) {
            pick = firstMatch(unrestricted, p -> true);
        }

        if (pick != null) {
            quota.record(pick);
            average.record(pick);
        }
        return pick;
    }

    /** Quota search order: in-range + in-position, then out-of-range + in-position, then either ignoring position. */
    private Player quotaMatch(List<Player> preferred, List<Player> unrestricted, Set<String> allowedPositions, Predicate<Player> threshold) {
        Player pick = firstMatch(preferred, p -> allowedPositions.contains(p.getPosition()) && threshold.test(p));
        if (pick == null) {
            pick = firstMatch(unrestricted, p -> allowedPositions.contains(p.getPosition()) && threshold.test(p));
        }
        if (pick == null) {
            pick = firstMatch(preferred, threshold);
        }
        if (pick == null) {
            pick = firstMatch(unrestricted, threshold);
        }
        return pick;
    }

    private Player firstMatch(List<Player> pool, Predicate<Player> predicate) {
        return pool.stream().filter(predicate).findFirst().orElse(null);
    }

    private Player closestByOverall(List<Player> pool, Set<String> allowedPositions, double ideal) {
        return pool.stream()
                .filter(p -> allowedPositions == null || allowedPositions.contains(p.getPosition()))
                .min(Comparator.comparingDouble(p -> Math.abs(p.getOverall() - ideal)))
                .orElse(null);
    }

    private void remove(List<Player> preferred, List<Player> unrestricted, Player player) {
        preferred.removeIf(p -> Objects.equals(p.getId(), player.getId()));
        unrestricted.removeIf(p -> Objects.equals(p.getId(), player.getId()));
    }

    /** Tracks how many more above/below-threshold players are still needed to satisfy OVERALL_DISTRIBUTION. */
    private static final class QuotaState {
        final int aboveThreshold;
        final int belowThreshold;
        int aboveRemaining;
        int belowRemaining;

        QuotaState(RosterBuildContext ctx) {
            this.aboveThreshold = ctx.aboveThreshold();
            this.belowThreshold = ctx.belowThreshold();
            this.aboveRemaining = ctx.aboveCount();
            this.belowRemaining = ctx.belowCount();
        }

        void record(Player p) {
            if (aboveRemaining > 0 && p.getOverall() >= aboveThreshold) {
                aboveRemaining--;
            }
            if (belowRemaining > 0 && p.getOverall() <= belowThreshold) {
                belowRemaining--;
            }
        }
    }

    /** Tracks the running average so each pick can target the value the remaining slots still need. */
    private static final class AverageState {
        final boolean active;
        final int targetSum;
        final int teamSize;
        int runningSum = 0;
        int slotsFilled = 0;

        AverageState(RosterBuildContext ctx) {
            this.active = ctx.targetAverage() > 0;
            this.targetSum = ctx.targetAverage() * ctx.teamSize();
            this.teamSize = ctx.teamSize();
        }

        double idealNext() {
            int remaining = teamSize - slotsFilled;
            return remaining <= 0 ? targetSum : (double) (targetSum - runningSum) / remaining;
        }

        void record(Player p) {
            runningSum += p.getOverall();
            slotsFilled++;
        }
    }
}
