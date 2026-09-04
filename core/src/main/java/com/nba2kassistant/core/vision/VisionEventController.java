package com.nba2kassistant.core.vision;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single event-shaped endpoint both {@code courtvision-vision}'s OCR
 * pipeline and the web app's manual tap-tracker POST to (plan §6) — an
 * unknown session is logged and dropped (202 either way), not an error:
 * a capture loop racing a session's teardown is an expected, harmless case,
 * not something worth failing the vision service's request over.
 */
@RestController
public class VisionEventController {

    private final VisionEventService visionEventService;

    public VisionEventController(VisionEventService visionEventService) {
        this.visionEventService = visionEventService;
    }

    @PostMapping("/api/vision/events")
    public ResponseEntity<Void> receive(@RequestBody VisionEventRequest request) {
        visionEventService.handle(request);
        return ResponseEntity.accepted().build();
    }
}
