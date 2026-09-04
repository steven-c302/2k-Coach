package com.nba2kassistant.core.matchup;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/** Post-position strength vs. post defense differential → ATTACK_INSIDE (plan §5). */
@Component
public class PostSizeRule implements MismatchRule {

    private static final Set<String> POST_POSITIONS = Set.of("PF", "C");

    @Override
    public List<Mismatch> evaluate(TeamSnapshot teamA, TeamSnapshot teamB) {
        List<Mismatch> mismatches = new ArrayList<>();
        checkDirection(teamA, teamB).ifPresent(mismatches::add);
        checkDirection(teamB, teamA).ifPresent(mismatches::add);
        return mismatches;
    }

    private Optional<Mismatch> checkDirection(TeamSnapshot attacking, TeamSnapshot defending) {
        List<PlayerSnapshot> attackingBigs = postPlayers(attacking);
        List<PlayerSnapshot> defendingBigs = postPlayers(defending);
        OptionalDouble strengthAvg = Snapshots.average(attackingBigs, PlayerAttributesSnapshot::strength);
        OptionalDouble defenseAvg = Snapshots.average(defendingBigs, PlayerAttributesSnapshot::postDefense);
        if (strengthAvg.isEmpty() || defenseAvg.isEmpty()) {
            return Optional.empty();
        }
        double gap = strengthAvg.getAsDouble() - defenseAvg.getAsDouble();
        if (gap < Severity.MIN_REPORTABLE_GAP) {
            return Optional.empty();
        }
        return Optional.of(new Mismatch(
                "ATTACK_INSIDE",
                attacking.label(),
                Severity.of(gap),
                "%s bigs avg strength %.1f vs %s bigs avg post defense %.1f (%.1f pt gap)".formatted(
                        attacking.label(), strengthAvg.getAsDouble(), defending.label(), defenseAvg.getAsDouble(), gap)
        ));
    }

    private static List<PlayerSnapshot> postPlayers(TeamSnapshot team) {
        return team.players().stream().filter(p -> POST_POSITIONS.contains(p.position())).toList();
    }
}
