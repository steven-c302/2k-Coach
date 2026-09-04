package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.roster.dto.GeneratedRosterResponse;
import com.nba2kassistant.core.roster.dto.RosterGenerateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RosterController {

    private final RosterGenerationService rosterGenerationService;

    public RosterController(RosterGenerationService rosterGenerationService) {
        this.rosterGenerationService = rosterGenerationService;
    }

    @PostMapping("/api/rosters/generate")
    public GeneratedRosterResponse generate(@Valid @RequestBody RosterGenerateRequest request) {
        return rosterGenerationService.generate(request);
    }

    @ExceptionHandler(RosterGenerationException.class)
    public ResponseEntity<Map<String, String>> handleGenerationError(RosterGenerationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }
}
