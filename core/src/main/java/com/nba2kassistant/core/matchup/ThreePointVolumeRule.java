package com.nba2kassistant.core.matchup;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/** teamA avg three_pt − teamB avg perimeter_defense > threshold → SHOOT_MORE_THREES (plan §5). */
@Component
public class ThreePointVolumeRule implements MismatchRule {

    @Override
    public List<Mismatch> evaluate(TeamSnapshot teamA, TeamSnapshot teamB) {
        List<Mismatch> mismatches = new ArrayList<>();
        checkDirection(teamA, teamB).ifPresent(mismatches::add);
        checkDirection(teamB, teamA).ifPresent(mismatches::add);
        return mismatches;
    }

    private Optional<Mismatch> checkDirection(TeamSnapshot shooting, TeamSnapshot defending) {
        OptionalDouble shootingAvg = Snapshots.average(shooting.players(), PlayerAttributesSnapshot::threePt);
        OptionalDouble defenseAvg = Snapshots.average(defending.players(), PlayerAttributesSnapshot::perimeterDefense);
        if (shootingAvg.isEmpty() || defenseAvg.isEmpty()) {
            return Optional.empty();
        }
        double gap = shootingAvg.getAsDouble() - defenseAvg.getAsDouble();
        if (gap < Severity.MIN_REPORTABLE_GAP) {
            return Optional.empty();
        }
        return Optional.of(new Mismatch(
                "SHOOT_MORE_THREES",
                shooting.label(),
                Severity.of(gap),
                "%s avg 3PT %.1f vs %s avg perimeter defense %.1f (%.1f pt gap)".formatted(
                        shooting.label(), shootingAvg.getAsDouble(), defending.label(), defenseAvg.getAsDouble(), gap)
        ));
    }
}
