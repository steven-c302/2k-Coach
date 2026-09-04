package com.nba2kassistant.core.player;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/api/players")
    public List<PlayerResponse> getPlayers(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Integer minOverall,
            @RequestParam(required = false) Integer maxOverall,
            @RequestParam(required = false) String era,
            @RequestParam(required = false) String name
    ) {
        return playerService.search(new PlayerSearchRequest(position, minOverall, maxOverall, era, name));
    }
}
