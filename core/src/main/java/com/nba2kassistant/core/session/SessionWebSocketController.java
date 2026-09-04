package com.nba2kassistant.core.session;

import com.nba2kassistant.core.session.dto.JoinSessionMessage;
import com.nba2kassistant.core.session.dto.ReadyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket/STOMP frames route by session code to that session's actor queue
 * (plan §4) — every handler here just resolves the actor and submits a pure
 * state-transition function; SessionActor guarantees only one mutation for a
 * given session is ever in flight at a time.
 */
@Controller
public class SessionWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(SessionWebSocketController.class);

    private final SessionRegistry sessionRegistry;

    public SessionWebSocketController(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @MessageMapping("/sessions/{code}/join")
    public void join(@DestinationVariable String code, JoinSessionMessage message) {
        withActor(code, actor -> actor.submit(state ->
                message.role() == JoinSessionMessage.ClientRole.HOST
                        ? state.withHost(message.clientId())
                        : state.withGuest(message.clientId())));
    }

    @MessageMapping("/sessions/{code}/ready")
    public void ready(@DestinationVariable String code, ReadyMessage message) {
        withActor(code, actor -> actor.submit(state -> state.withReady(message.clientId(), message.ready())));
    }

    private void withActor(String code, java.util.function.Consumer<SessionActor> action) {
        sessionRegistry.find(code).ifPresentOrElse(
                action,
                () -> log.warn("Received a message for unknown session {}", code)
        );
    }
}
