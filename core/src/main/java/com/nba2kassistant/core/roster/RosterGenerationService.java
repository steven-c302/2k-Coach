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
        pipeline.add(new EraCriterion(era));
        pipeline.addAll(resolution.criteria());
        boolean hasOrderingCriterion = resolution.criteria().stream()
                .anyMatch(c -> c instanceof BuildAroundPlayerCriterion);
        if (!hasOrderingCriterion) {
            pipeline.add(new RandomShuffleCriterion());
        }

        RosterBuildContext ctx = new RosterBuildContext(request.teamSize(), era, resolution.anchor());

        List<Player> candidates = playerRepository.findAll();
        for (RosterCriterion criterion : pipeline) {
            candidates = criterion.apply(candidates, ctx);
        }

        GeneratedRoster roster = fillService.fill(candidates, ctx);

        return new GeneratedRosterResponse(
                roster.starters().stream().map(PlayerResponse::from).toList(),
                roster.bench().stream().map(PlayerResponse::from).toList()
        );
    }
}
