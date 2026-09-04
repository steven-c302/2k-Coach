package com.nba2kassistant.core.matchup;

/** Shared severity bucketing so every rule's thresholds mean the same thing. */
final class Severity {

    static final double MIN_REPORTABLE_GAP = 8;
    private static final double MEDIUM_GAP = 10;
    private static final double HIGH_GAP = 15;

    private Severity() {
    }

    static String of(double gap) {
        double magnitude = Math.abs(gap);
        if (magnitude >= HIGH_GAP) {
            return "HIGH";
        }
        if (magnitude >= MEDIUM_GAP) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
