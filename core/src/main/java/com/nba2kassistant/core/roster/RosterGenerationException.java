package com.nba2kassistant.core.roster;

/** Thrown when the filtered candidate pool can't fill the requested roster (see RosterController). */
public class RosterGenerationException extends RuntimeException {

    public RosterGenerationException(String message) {
        super(message);
    }
}
