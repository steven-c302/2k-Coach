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
        roster.addAll(pool.subList(0, benchSpotsNeeded));

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
}
