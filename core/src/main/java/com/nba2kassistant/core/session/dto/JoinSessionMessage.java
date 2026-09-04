package com.nba2kassistant.core.session.dto;

public record JoinSessionMessage(String clientId, ClientRole role) {

    public enum ClientRole {
        HOST, GUEST
    }
}
