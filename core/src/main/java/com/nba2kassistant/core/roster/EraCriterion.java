package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;

import java.util.List;
import java.util.Objects;

/**
 * One of the four criterion implementations named in NBA2K Assistant plan
 * §3. The top-level {@code era} field on a generate request is translated
 * into this criterion and always run first (see RosterGenerationService) —
 * except when era is "ALL", in which case RosterGenerationService skips
 * this criterion entirely so current/classic/all-time players can mix.
 */
public final class EraCriterion implements RosterCriterion {

    private final String eraTag;

    public EraCriterion(String eraTag) {
        this.eraTag = eraTag;
    }

    @Override
    public List<Player> apply(List<Player> candidates, RosterBuildContext ctx) {
        return candidates.stream()
                .filter(p -> Objects.equals(p.getEraTag(), eraTag))
                .toList();
    }
}
