package com.nba2kassistant.core.session;

import com.nba2kassistant.core.session.dto.SessionStateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The only genuinely shared concurrent structure in the live-session design
 * (plan §4): a session code -> {@link SessionActor} map, used only for
 * independent-key lookup/insert. It deliberately does NOT hold session state
 * itself — a plain concurrent map doesn't protect the multi-field invariants
 * ({@code hostReady}/{@code guestReady}/{@code status} must move together)
 * that {@link SessionActor} exists to guard.
 */
@Component
public class SessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SessionRegistry.class);

    private final ConcurrentHashMap<String, SessionActor> actors = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRepository sessionRepository;

    public SessionRegistry(SimpMessagingTemplate messagingTemplate, SessionRepository sessionRepository) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRepository = sessionRepository;
    }

    public SessionActor register(SessionState initial) {
        SessionActor actor = new SessionActor(initial, state -> broadcastAndPersist(initial.code(), state));
        actors.put(initial.code(), actor);
        return actor;
    }

    public Optional<SessionActor> find(String code) {
        return Optional.ofNullable(actors.get(code));
    }

    private void broadcastAndPersist(String code, SessionState state) {
        messagingTemplate.convertAndSend("/topic/sessions/" + code, SessionStateResponse.from(state));

        sessionRepository.findByCode(code).ifPresentOrElse(entity -> {
            entity.setState(state.status().name());
            entity.setHostClientId(state.hostClientId());
            entity.setGuestClientId(state.guestClientId());
            entity.setVersion(state.version());
            sessionRepository.save(entity);
        }, () -> log.warn("Session {} has an active actor but no persisted row", code));
    }
}
