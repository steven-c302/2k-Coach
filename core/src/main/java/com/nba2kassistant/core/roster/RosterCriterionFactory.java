package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import com.nba2kassistant.core.player.PlayerRepository;
import com.nba2kassistant.core.roster.dto.BuildAroundCriterionSpec;
import com.nba2kassistant.core.roster.dto.ExcludeIdsCriterionSpec;
import com.nba2kassistant.core.roster.dto.OverallDistributionCriterionSpec;
import com.nba2kassistant.core.roster.dto.OverallRangeCriterionSpec;
import com.nba2kassistant.core.roster.dto.PositionCriterionSpec;
import com.nba2kassistant.core.roster.dto.RosterCriterionSpec;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Resolves wire-format {@link RosterCriterionSpec}s into domain {@link RosterCriterion}s. */
@Component
public class RosterCriterionFactory {

    private final PlayerRepository playerRepository;

    public RosterCriterionFactory(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    /**
     * @param anchor the resolved BUILD_AROUND player, if the specs included one; null otherwise
     * @param aboveThreshold/aboveCount/belowThreshold/belowCount the OVERALL_DISTRIBUTION quota, if
     *        present; a null/zero count means that half of the quota is disabled
     */
    public record Resolution(
            List<RosterCriterion> criteria,
            Player anchor,
            Integer aboveThreshold,
            Integer aboveCount,
            Integer belowThreshold,
            Integer belowCount
    ) {
    }

    public Resolution resolve(List<RosterCriterionSpec> specs) {
        List<RosterCriterion> criteria = new ArrayList<>();
        Player anchor = null;
        Integer aboveThreshold = null;
        Integer aboveCount = null;
        Integer belowThreshold = null;
        Integer belowCount = null;

        for (RosterCriterionSpec spec : specs) {
            switch (spec) {
                case OverallRangeCriterionSpec s -> criteria.add(new OverallRangeCriterion(s.min(), s.max()));
                case PositionCriterionSpec s -> criteria.add(new PositionCriterion(new HashSet<>(s.allowed())));
                case BuildAroundCriterionSpec s -> {
                    Player player = playerRepository.findById(s.playerId())
                            .orElseThrow(() -> new RosterGenerationException("BUILD_AROUND player not found: " + s.playerId()));
                    criteria.add(new BuildAroundPlayerCriterion(player));
                    anchor = player;
                }
                case ExcludeIdsCriterionSpec s -> criteria.add(new ExcludeIdsCriterion(new HashSet<>(s.ids())));
                case OverallDistributionCriterionSpec s -> {
                    aboveThreshold = s.aboveThreshold();
                    aboveCount = s.aboveCount();
                    belowThreshold = s.belowThreshold();
                    belowCount = s.belowCount();
                }
            }
        }

        return new Resolution(criteria, anchor, aboveThreshold, aboveCount, belowThreshold, belowCount);
    }
}
