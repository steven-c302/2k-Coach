package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.common.EraTag;
import com.nba2kassistant.core.player.Player;
import com.nba2kassistant.core.player.PlayerRepository;
import com.nba2kassistant.core.player.PlayerResponse;
import com.nba2kassistant.core.roster.dto.GeneratedRosterResponse;
import com.nba2kassistant.core.roster.dto.RosterGenerateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RosterGenerationService {

    private final PlayerRepository playerRepository;
    private final RosterCriterionFactory criterionFactory;
    private final RosterFillService fillService;

    public RosterGenerationService(
            PlayerRepository playerRepository,
            RosterCriterionFactory criterionFactory,
            RosterFillService fillService
    ) {
        this.playerRepository = playerRepository;
        this.criterionFactory = criterionFactory;
        this.fillService = fillService;
    }

    @Transactional(readOnly = true)
    public GeneratedRosterResponse generate(RosterGenerateRequest request) {
        String era = request.era() == null || request.era().isBlank() ? EraTag.CURRENT : request.era();
        RosterCriterionFactory.Resolution resolution = criterionFactory.resolve(
                request.criteria() == null ? List.of() : request.criteria());

        List<RosterCriterion> pipeline = new ArrayList<>();
        // "ALL" opts into mixing current/classic/all-time players on one roster; every other
        // value (including the CURRENT default) still restricts to a single era, unchanged.
        if (!"ALL".equals(era)) {
            pipeline.add(new EraCriterion(era));
        }
        pipeline.addAll(resolution.criteria());
        boolean hasOrderingCriterion = resolution.criteria().stream()
                .anyMatch(c -> c instanceof BuildAroundPlayerCriterion);
        if (!hasOrderingCriterion) {
            pipeline.add(new RandomShuffleCriterion());
        }

        RosterBuildContext ctx = new RosterBuildContext(
                request.teamSize(),
                era,
                resolution.anchor(),
                resolution.aboveThreshold() == null ? 0 : resolution.aboveThreshold(),
                resolution.aboveCount() == null ? 0 : resolution.aboveCount(),
                resolution.belowThreshold() == null ? 0 : resolution.belowThreshold(),
                resolution.belowCount() == null ? 0 : resolution.belowCount(),
                resolution.targetAverage() == null ? 0 : resolution.targetAverage()
        );

        List<Player> candidates = playerRepository.findAll();
        for (RosterCriterion criterion : pipeline) {
            candidates = criterion.apply(candidates, ctx);
        }

        // A quota/average target needs to be able to reach outside a stated OVERALL_RANGE (e.g.
        // "2 players >= 90" with a 60-80 range, or "average 70" with the same) - this reruns the
        // same pipeline minus the range filter so RosterFillService has somewhere to look when the
        // range-restricted pool can't satisfy one of those on its own.
        boolean needsFallbackPool = ctx.aboveCount() > 0 || ctx.belowCount() > 0 || ctx.targetAverage() > 0;
        List<Player> unrestricted = candidates;
        if (needsFallbackPool && resolution.rangeMin() != null) {
            List<RosterCriterion> fallbackPipeline = pipeline.stream()
                    .filter(c -> !(c instanceof OverallRangeCriterion))
                    .toList();
            unrestricted = playerRepository.findAll();
            for (RosterCriterion criterion : fallbackPipeline) {
                unrestricted = criterion.apply(unrestricted, ctx);
            }
        }

        GeneratedRoster roster = fillService.fill(candidates, unrestricted, ctx);

        return new GeneratedRosterResponse(
                roster.starters().stream().map(PlayerResponse::from).toList(),
                roster.bench().stream().map(PlayerResponse::from).toList()
        );
    }
}
