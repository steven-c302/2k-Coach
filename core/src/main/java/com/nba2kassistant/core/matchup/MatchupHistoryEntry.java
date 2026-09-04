package com.nba2kassistant.core.matchup;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * DynamoDB item for one completed matchup analysis — PK sessionCode
 * ("STANDALONE" when not tied to a live session, e.g. a bare API call), SK
 * {@code <timestamp>#MATCHUP_ANALYZED} (plan §2). {@code finalScore} stays
 * null until a live score feed exists (Milestone 10's OCR pipeline) — there
 * is no live score tracking yet, and this field isn't faked to look complete.
 */
@DynamoDbBean
public class MatchupHistoryEntry {

    private String sessionCode;
    private String timestampEventType;
    private String teamAPlayerIds;
    private String teamBPlayerIds;
    private String mismatchSummary;
    private String finalScore;

    public static MatchupHistoryEntry of(
            String sessionCode, String timestampEventType,
            String teamAPlayerIds, String teamBPlayerIds, String mismatchSummary
    ) {
        MatchupHistoryEntry entry = new MatchupHistoryEntry();
        entry.setSessionCode(sessionCode);
        entry.setTimestampEventType(timestampEventType);
        entry.setTeamAPlayerIds(teamAPlayerIds);
        entry.setTeamBPlayerIds(teamBPlayerIds);
        entry.setMismatchSummary(mismatchSummary);
        return entry;
    }

    @DynamoDbPartitionKey
    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    @DynamoDbSortKey
    public String getTimestampEventType() {
        return timestampEventType;
    }

    public void setTimestampEventType(String timestampEventType) {
        this.timestampEventType = timestampEventType;
    }

    public String getTeamAPlayerIds() {
        return teamAPlayerIds;
    }

    public void setTeamAPlayerIds(String teamAPlayerIds) {
        this.teamAPlayerIds = teamAPlayerIds;
    }

    public String getTeamBPlayerIds() {
        return teamBPlayerIds;
    }

    public void setTeamBPlayerIds(String teamBPlayerIds) {
        this.teamBPlayerIds = teamBPlayerIds;
    }

    public String getMismatchSummary() {
        return mismatchSummary;
    }

    public void setMismatchSummary(String mismatchSummary) {
        this.mismatchSummary = mismatchSummary;
    }

    public String getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(String finalScore) {
        this.finalScore = finalScore;
    }
}
