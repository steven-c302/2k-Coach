package com.nba2kassistant.core.matchup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MatchupHistoryController {

    private final MatchupHistoryRepository matchupHistoryRepository;

    public MatchupHistoryController(MatchupHistoryRepository matchupHistoryRepository) {
        this.matchupHistoryRepository = matchupHistoryRepository;
    }

    @GetMapping("/api/matchups/history/{sessionCode}")
    public List<MatchupHistoryEntry> history(@PathVariable String sessionCode) {
        return matchupHistoryRepository.findBySessionCode(sessionCode);
    }
}
