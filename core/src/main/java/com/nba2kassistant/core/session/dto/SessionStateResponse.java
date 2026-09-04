package com.nba2kassistant.core.session.dto;

import com.nba2kassistant.core.session.SessionState;
import com.nba2kassistant.core.session.SessionStatus;

public record SessionStateResponse(
        String code,
        SessionStatus status,
        String hostClientId,
        String guestClientId,
        boolean hostReady,
        boolean guestReady,
        String lastNarration,
        long version
) {
    public static SessionStateResponse from(SessionState state) {
        return new SessionStateResponse(
                state.code(), state.status(), state.hostClientId(), state.guestClientId(),
                state.hostReady(), state.guestReady(), state.lastNarration(), state.version()
        );
    }
}
