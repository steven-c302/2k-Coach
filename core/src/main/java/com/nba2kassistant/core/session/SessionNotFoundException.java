package com.nba2kassistant.core.session;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String code) {
        super("No active session for code " + code);
    }
}
