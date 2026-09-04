package com.nba2kassistant.core.coaching;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The demoable artifact for the async/versioning claim — query this and see DISCARDED_STALE entries next to DELIVERED ones. */
@RestController
public class CoachingEventLogController {

    private final CoachingEventLogRepository coachingEventLogRepository;

    public CoachingEventLogController(CoachingEventLogRepository coachingEventLogRepository) {
        this.coachingEventLogRepository = coachingEventLogRepository;
    }

    @GetMapping("/api/sessions/{code}/coaching-log")
    public List<CoachingEventLogEntry> log(@PathVariable String code) {
        return coachingEventLogRepository.findBySessionCode(code);
    }
}
