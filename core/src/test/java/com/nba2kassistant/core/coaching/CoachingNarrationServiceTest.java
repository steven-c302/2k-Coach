package com.nba2kassistant.core.coaching;

import com.nba2kassistant.core.matchup.MatchupAnalysisService;
import com.nba2kassistant.core.matchup.dto.MatchupAnalysisResponse;
import com.nba2kassistant.core.player.PlayerRepository;
import com.nba2kassistant.core.session.SessionActor;
import com.nba2kassistant.core.session.SessionState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The plan's highest-resume-value test (§8): a delayed response (A) must be
 * discarded and logged DISCARDED_STALE if a newer request (B) has already
 * resolved and been delivered by the time A finally returns. Uses a fake,
 * controllable-delay LlmClient — never a real API call, matching the plan's
 * explicit testing guidance.
 */
@ExtendWith(MockitoExtension.class)
class CoachingNarrationServiceTest {

    @Mock
    private MatchupAnalysisService matchupAnalysisService;
    @Mock
    private CoachingEventLogRepository coachingEventLogRepository;
    @Mock
    private PlayerRepository playerRepository;

    private final ExecutorService llmExecutor = Executors.newFixedThreadPool(4);

    @AfterEach
    void shutdownExecutor() {
        llmExecutor.shutdown();
    }

    @Test
    void discardsAStaleResponseWhenANewerRequestIsDeliveredFirst() throws InterruptedException {
        when(matchupAnalysisService.analyze(any())).thenReturn(new MatchupAnalysisResponse(List.of()));
        when(playerRepository.findAllById(any())).thenReturn(List.of());

        SessionActor actor = new SessionActor(SessionState.newLobby("ABC123"), state -> {
        });
        long requestVersionA = actor.submit(SessionState::withVersionBump).join().version();

        CountDownLatch aStarted = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);

        LlmClient fakeLlmClient = context -> {
            if (callCount.incrementAndGet() == 1) {
                aStarted.countDown();
                awaitUninterruptibly(releaseA);
                return "narration-A";
            }
            return "narration-B";
        };

        CoachingNarrationService service = new CoachingNarrationService(
                matchupAnalysisService, fakeLlmClient, coachingEventLogRepository, playerRepository, llmExecutor);

        // Request A starts and blocks inside the "LLM call" — simulating a slow response.
        service.requestNarration("ABC123", requestVersionA, List.of(1L), List.of(2L), actor);
        assertThat(aStarted.await(2, TimeUnit.SECONDS)).as("request A's LLM call to have started").isTrue();

        // A new game state arrives before A resolves: version bumps, request B fires and resolves fast.
        long requestVersionB = actor.submit(SessionState::withVersionBump).join().version();
        service.requestNarration("ABC123", requestVersionB, List.of(3L), List.of(4L), actor);

        // B should be delivered and broadcast before A ever returns.
        verify(coachingEventLogRepository, timeout(2000)).save(argThatMatchesVersion(requestVersionB, CoachingEventLogEntry.DELIVERED));
        assertThat(actor.currentState().lastNarration()).isEqualTo("narration-B");

        // Now let the stale A finally resolve.
        releaseA.countDown();
        verify(coachingEventLogRepository, timeout(2000)).save(argThatMatchesVersion(requestVersionA, CoachingEventLogEntry.DISCARDED_STALE));

        // A's narration must never have overwritten B's on the live session.
        assertThat(actor.currentState().lastNarration()).isEqualTo("narration-B");

        actor.shutdown();
    }

    private static CoachingEventLogEntry argThatMatchesVersion(long sequenceNumber, String status) {
        return org.mockito.ArgumentMatchers.argThat(entry ->
                entry != null && entry.getSequenceNumber() == sequenceNumber && status.equals(entry.getStatus()));
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
