package com.nba2kassistant.core.matchup;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Speed differential → DRIVE_MORE (plan §5). The plan's original spec compares
 * speed against a "lateral quickness" defensive attribute, but that isn't one
 * of the 8 attributes this schema tracks (see player_attributes) — adapted to
 * a straight speed-vs-speed comparison instead of inventing an untracked field.
 */
@Component
public class SpeedMismatchRule implements MismatchRule {

    @Override
    public List<Mismatch> evaluate(TeamSnapshot teamA, TeamSnapshot teamB) {
        List<Mismatch> mismatches = new ArrayList<>();
        checkDirection(teamA, teamB).ifPresent(mismatches::add);
        checkDirection(teamB, teamA).ifPresent(mismatches::add);
        return mismatches;
    }

    private Optional<Mismatch> checkDirection(TeamSnapshot faster, TeamSnapshot slower) {
        OptionalDouble fasterAvg = Snapshots.average(faster.players(), PlayerAttributesSnapshot::speed);
        OptionalDouble slowerAvg = Snapshots.average(slower.players(), PlayerAttributesSnapshot::speed);
        if (fasterAvg.isEmpty() || slowerAvg.isEmpty()) {
            return Optional.empty();
        }
        double gap = fasterAvg.getAsDouble() - slowerAvg.getAsDouble();
        if (gap < Severity.MIN_REPORTABLE_GAP) {
            return Optional.empty();
        }
        return Optional.of(new Mismatch(
                "DRIVE_MORE",
                faster.label(),
                Severity.of(gap),
                "%s avg speed %.1f vs %s avg speed %.1f (%.1f pt gap)".formatted(
                        faster.label(), fasterAvg.getAsDouble(), slower.label(), slowerAvg.getAsDouble(), gap)
        ));
    }
}
