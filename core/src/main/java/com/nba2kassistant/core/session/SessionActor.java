package com.nba2kassistant.core.session;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Per-session single-writer actor (plan §4). Every mutation is submitted as a
 * task to this session's own single-thread virtual-thread executor, so only
 * one thread ever touches this session's state — correctness by construction,
 * no lock-ordering to reason about across the multiple fields
 * ({@code hostReady}/{@code guestReady}/{@code status}) that must move
 * together. {@code onStateChanged} fires after each processed mutation with
 * the new immutable snapshot, for broadcasting.
 *
 * <p>Java 21 makes one-thread-per-session cheap at this scale (a virtual
 * thread, not an OS thread) — see SessionRegistry for why a single
 * {@code ConcurrentHashMap} is enough for the one genuinely shared structure
 * (session code → actor), and is deliberately NOT used to hold state
 * directly.
 */
public class SessionActor {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    private final Consumer<SessionState> onStateChanged;
    private volatile SessionState state;

    public SessionActor(SessionState initial, Consumer<SessionState> onStateChanged) {
        this.state = initial;
        this.onStateChanged = onStateChanged;
    }

    /** Last state a completed mutation produced — may be briefly stale while a mutation is in flight. */
    public SessionState currentState() {
        return state;
    }

    public CompletableFuture<SessionState> submit(UnaryOperator<SessionState> mutation) {
        return CompletableFuture.supplyAsync(() -> {
            SessionState next = mutation.apply(state);
            state = next;
            onStateChanged.accept(next);
            return next;
        }, executor);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
