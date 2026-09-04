package com.nba2kassistant.core.session.dto;

import com.nba2kassistant.core.session.SessionState;
import com.nba2kassistant.core.session.SessionStatus;

import java.util.Map;

public record SessionStateResponse(
        String code,
        SessionStatus status,
        String hostClientId,
        String guestClientId,
        boolean hostReady,
        boolean guestReady,
        String lastNarration,
        Map<String, Object> liveGameState,
        long version
) {
    public static SessionStateResponse from(SessionState state) {
        return new SessionStateResponse(
                state.code(), state.status(), state.hostClientId(), state.guestClientId(),
                state.hostReady(), state.guestReady(), state.lastNarration(), state.liveGameState(), state.version()
        );
    }
}
