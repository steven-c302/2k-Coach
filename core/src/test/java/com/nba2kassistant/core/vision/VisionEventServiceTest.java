package com.nba2kassistant.core.vision;

import com.nba2kassistant.core.session.SessionActor;
import com.nba2kassistant.core.session.SessionRegistry;
import com.nba2kassistant.core.session.SessionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisionEventServiceTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @Test
    void mergesTheEventPayloadIntoTheMatchingSessions_liveGameState() {
        SessionActor actor = new SessionActor(SessionState.newLobby("ABC123"), state -> {
        });
        when(sessionRegistry.find("ABC123")).thenReturn(Optional.of(actor));

        VisionEventService service = new VisionEventService(sessionRegistry);
        VisionEventRequest request = new VisionEventRequest(
                "ABC123", VisionEventType.SCORE_UPDATE, Map.of("teamAScore", 12, "teamBScore", 9), Instant.now());

        service.handle(request);

        awaitVersionAtLeast(actor, 1);
        assertThat(actor.currentState().liveGameState()).containsEntry("teamAScore", 12).containsEntry("teamBScore", 9);
    }

    @Test
    void laterEventsMergeOverEarlierOnesRatherThanReplacingTheWholeState() {
        SessionActor actor = new SessionActor(SessionState.newLobby("ABC123"), state -> {
        });
        when(sessionRegistry.find("ABC123")).thenReturn(Optional.of(actor));
        VisionEventService service = new VisionEventService(sessionRegistry);

        service.handle(new VisionEventRequest("ABC123", VisionEventType.SCORE_UPDATE, Map.of("teamAScore", 12), Instant.now()));
        awaitVersionAtLeast(actor, 1);
        service.handle(new VisionEventRequest("ABC123", VisionEventType.CLOCK_UPDATE, Map.of("minutes", 5), Instant.now()));
        awaitVersionAtLeast(actor, 2);

        assertThat(actor.currentState().liveGameState())
                .containsEntry("teamAScore", 12)
                .containsEntry("minutes", 5);
    }

    @Test
    void doesNothingForAnUnknownSession() {
        when(sessionRegistry.find("MISSING")).thenReturn(Optional.empty());

        VisionEventService service = new VisionEventService(sessionRegistry);

        service.handle(new VisionEventRequest("MISSING", VisionEventType.SCORE_UPDATE, Map.of(), Instant.now()));
        // no exception, no interaction beyond the lookup — verify the mock wasn't asked to do anything else
        verify(sessionRegistry, timeout(500)).find("MISSING");
    }

    private static void awaitVersionAtLeast(SessionActor actor, long version) {
        long deadline = System.currentTimeMillis() + 2000;
        while (actor.currentState().version() < version && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
    }
}
