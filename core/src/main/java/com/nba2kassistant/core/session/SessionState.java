package com.nba2kassistant.core.session;

/**
 * Immutable snapshot of one live session. Every transition produces a new
 * instance with {@code version} incremented — the same counter design
 * reused for LLM staleness detection in Milestone 6 (plan §4/§5): one
 * monotonic source of truth, not two that can drift.
 */
public record SessionState(
        String code,
        SessionStatus status,
        String hostClientId,
        String guestClientId,
        boolean hostReady,
        boolean guestReady,
        long version
) {

    public static SessionState newLobby(String code) {
        return new SessionState(code, SessionStatus.LOBBY, null, null, false, false, 0);
    }

    public SessionState withHost(String clientId) {
        return new SessionState(code, status, clientId, guestClientId, hostReady, guestReady, version + 1);
    }

    public SessionState withGuest(String clientId) {
        return new SessionState(code, status, hostClientId, clientId, hostReady, guestReady, version + 1);
    }

    public SessionState withReady(String clientId, boolean ready) {
        boolean isHost = clientId.equals(hostClientId);
        boolean isGuest = clientId.equals(guestClientId);
        boolean newHostReady = isHost ? ready : hostReady;
        boolean newGuestReady = isGuest ? ready : guestReady;
        boolean bothPresent = hostClientId != null && guestClientId != null;
        SessionStatus newStatus = bothPresent && newHostReady && newGuestReady ? SessionStatus.ACTIVE : SessionStatus.LOBBY;
        return new SessionState(code, newStatus, hostClientId, guestClientId, newHostReady, newGuestReady, version + 1);
    }
}
