package com.nba2kassistant.core.vision;

import com.nba2kassistant.core.session.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VisionEventService {

    private static final Logger log = LoggerFactory.getLogger(VisionEventService.class);

    private final SessionRegistry sessionRegistry;

    public VisionEventService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void handle(VisionEventRequest request) {
        sessionRegistry.find(request.sessionId()).ifPresentOrElse(
                actor -> actor.submit(state -> state.withGameStateUpdate(request.payload())),
                () -> log.warn("Vision event for unknown session {}", request.sessionId())
        );
    }
}
