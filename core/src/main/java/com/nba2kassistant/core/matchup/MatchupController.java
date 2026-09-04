package com.nba2kassistant.core.matchup;

import com.nba2kassistant.core.matchup.dto.MatchupAnalysisResponse;
import com.nba2kassistant.core.matchup.dto.MatchupAnalyzeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MatchupController {

    private final MatchupAnalysisService matchupAnalysisService;

    public MatchupController(MatchupAnalysisService matchupAnalysisService) {
        this.matchupAnalysisService = matchupAnalysisService;
    }

    @PostMapping("/api/matchups/analyze")
    public MatchupAnalysisResponse analyze(@RequestBody MatchupAnalyzeRequest request) {
        return matchupAnalysisService.analyze(request);
    }

    @ExceptionHandler(MatchupAnalysisException.class)
    public ResponseEntity<Map<String, String>> handleAnalysisError(MatchupAnalysisException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }
}
