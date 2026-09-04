package com.nba2kassistant.core.matchup.dto;

import com.nba2kassistant.core.matchup.Mismatch;

public record MismatchResponse(String category, String favoredTeam, String severity, String evidence) {

    public static MismatchResponse from(Mismatch mismatch) {
        return new MismatchResponse(mismatch.category(), mismatch.favoredTeam(), mismatch.severity(), mismatch.evidence());
    }
}
