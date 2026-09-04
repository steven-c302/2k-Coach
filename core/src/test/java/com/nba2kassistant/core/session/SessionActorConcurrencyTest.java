package com.nba2kassistant.core.session;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the thread-safety claim in plan §4/§8: N concurrent read-modify-write
 * mutations against one actor must produce the same final state as applying
 * them one at a time — no lost updates, no torn reads. A boolean toggle is
 * deliberately used (not a plain counter) because it's the shape that breaks
 * under naive unsynchronized concurrent access: two threads reading the same
 * stale value and both flipping it the same way loses an update silently.
 */
class SessionActorConcurrencyTest {

    private static final int MUTATION_COUNT = 500;
    private static final int SUBMITTER_THREADS = 50;

    @Test
    void concurrentTogglesProduceTheSameResultAsSequentialApplication() throws InterruptedException {
        SessionActor actor = new SessionActor(SessionState.newLobby("ABC123"), state -> {
        });

        ExecutorService submitters = Executors.newFixedThreadPool(SUBMITTER_THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<CompletableFuture<SessionState>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < MUTATION_COUNT; i++) {
                submitters.submit(() -> {
                    await(startGate);
                    synchronized (futures) {
                        futures.add(actor.submit(state -> state.withReady("host-1", !state.hostReady())));
                    }
                });
            }
            startGate.countDown(); // release every submitter thread at once to maximize contention

            submitters.shutdown();
            assertThat(submitters.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } finally {
            actor.shutdown();
        }

        SessionState finalState = actor.currentState();

        // MUTATION_COUNT toggles from an initial `false`: an even count returns to false.
        assertThat(finalState.version()).isEqualTo(MUTATION_COUNT);
        assertThat(finalState.hostReady()).isEqualTo(MUTATION_COUNT % 2 == 1);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
