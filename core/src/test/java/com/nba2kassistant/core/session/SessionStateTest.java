package com.nba2kassistant.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionStateTest {

    @Test
    void newLobbyStartsAtVersionZeroWithNoClients() {
        SessionState state = SessionState.newLobby("ABC123");

        assertThat(state.status()).isEqualTo(SessionStatus.LOBBY);
        assertThat(state.hostClientId()).isNull();
        assertThat(state.guestClientId()).isNull();
        assertThat(state.version()).isZero();
    }

    @Test
    void everyTransitionIncrementsVersion() {
        SessionState state = SessionState.newLobby("ABC123");

        SessionState afterHost = state.withHost("host-1");
        SessionState afterGuest = afterHost.withGuest("guest-1");

        assertThat(afterHost.version()).isEqualTo(1);
        assertThat(afterGuest.version()).isEqualTo(2);
    }

    @Test
    void becomesActiveOnlyWhenBothClientsPresentAndReady() {
        SessionState state = SessionState.newLobby("ABC123")
                .withHost("host-1")
                .withGuest("guest-1");

        SessionState hostReady = state.withReady("host-1", true);
        assertThat(hostReady.status()).isEqualTo(SessionStatus.LOBBY); // guest not ready yet

        SessionState bothReady = hostReady.withReady("guest-1", true);
        assertThat(bothReady.status()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void cannotBecomeActiveWithOnlyOneClientEvenIfMarkedReady() {
        SessionState state = SessionState.newLobby("ABC123").withHost("host-1");

        SessionState result = state.withReady("host-1", true);

        assertThat(result.status()).isEqualTo(SessionStatus.LOBBY);
    }

    @Test
    void unreadyingEitherClientDropsBackToLobby() {
        SessionState active = SessionState.newLobby("ABC123")
                .withHost("host-1")
                .withGuest("guest-1")
                .withReady("host-1", true)
                .withReady("guest-1", true);
        assertThat(active.status()).isEqualTo(SessionStatus.ACTIVE);

        SessionState result = active.withReady("guest-1", false);

        assertThat(result.status()).isEqualTo(SessionStatus.LOBBY);
        assertThat(result.hostReady()).isTrue();
        assertThat(result.guestReady()).isFalse();
    }

    @Test
    void isImmutableEachTransitionReturnsANewInstance() {
        SessionState original = SessionState.newLobby("ABC123");

        SessionState mutated = original.withHost("host-1");

        assertThat(original.hostClientId()).isNull();
        assertThat(mutated.hostClientId()).isEqualTo("host-1");
    }

    @Test
    void withVersionBumpChangesOnlyTheVersion() {
        SessionState state = SessionState.newLobby("ABC123").withHost("host-1");

        SessionState bumped = state.withVersionBump();

        assertThat(bumped.version()).isEqualTo(state.version() + 1);
        assertThat(bumped.hostClientId()).isEqualTo(state.hostClientId());
        assertThat(bumped.status()).isEqualTo(state.status());
    }

    @Test
    void withNarrationSetsTheNarrationAndBumpsVersion() {
        SessionState state = SessionState.newLobby("ABC123");

        SessionState narrated = state.withNarration("shoot more threes");

        assertThat(narrated.lastNarration()).isEqualTo("shoot more threes");
        assertThat(narrated.version()).isEqualTo(state.version() + 1);
    }

    @Test
    void newLobbyStartsWithEmptyLiveGameState() {
        assertThat(SessionState.newLobby("ABC123").liveGameState()).isEmpty();
    }

    @Test
    void withGameStateUpdateMergesRatherThanReplaces() {
        SessionState state = SessionState.newLobby("ABC123")
                .withGameStateUpdate(java.util.Map.of("teamAScore", 10))
                .withGameStateUpdate(java.util.Map.of("minutes", 5));

        assertThat(state.liveGameState())
                .containsEntry("teamAScore", 10)
                .containsEntry("minutes", 5);
    }

    @Test
    void withGameStateUpdateOverwritesAKeyItRepeats() {
        SessionState state = SessionState.newLobby("ABC123")
                .withGameStateUpdate(java.util.Map.of("teamAScore", 10))
                .withGameStateUpdate(java.util.Map.of("teamAScore", 12));

        assertThat(state.liveGameState()).containsEntry("teamAScore", 12);
    }
}
