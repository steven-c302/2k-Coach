package com.nba2kassistant.core.player;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerRepository playerRepository;

    public PlayerController(PlayerService playerService, PlayerRepository playerRepository) {
        this.playerService = playerService;
        this.playerRepository = playerRepository;
    }

    @GetMapping("/api/players")
    public List<PlayerResponse> getPlayers(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Integer minOverall,
            @RequestParam(required = false) Integer maxOverall,
            @RequestParam(required = false) String era,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer minThreePt,
            @RequestParam(required = false) Integer minMidRange,
            @RequestParam(required = false) Integer minLayup,
            @RequestParam(required = false) Integer minDunk,
            @RequestParam(required = false) Integer minSpeed,
            @RequestParam(required = false) Integer minStrength,
            @RequestParam(required = false) Integer minPostDefense,
            @RequestParam(required = false) Integer minPerimeterDefense
    ) {
        return playerService.search(new PlayerSearchRequest(
                position, minOverall, maxOverall, era, team, name,
                minThreePt, minMidRange, minLayup, minDunk, minSpeed, minStrength, minPostDefense, minPerimeterDefense
        ));
    }

    @GetMapping("/api/players/eras")
    public List<String> getEras() {
        return playerRepository.findDistinctEraTags();
    }

    @GetMapping("/api/players/teams")
    public List<String> getTeams(@RequestParam String era) {
        return playerRepository.findDistinctTeamsByEraTag(era);
    }
}
