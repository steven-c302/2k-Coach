package com.nba2kassistant.core.coaching;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * DynamoDB item for one narration request/response — PK sessionCode, SK the
 * requestVersion (plan §2). This table is the interview artifact for the
 * async/versioning claim: querying it and showing a DISCARDED_STALE entry is
 * concrete, demoable proof of the race handling, not just a code claim.
 */
@DynamoDbBean
public class CoachingEventLogEntry {

    public static final String DELIVERED = "DELIVERED";
    public static final String DISCARDED_STALE = "DISCARDED_STALE";

    private String sessionCode;
    private Long sequenceNumber;
    private String requestedAt;
    private String respondedAt;
    private String ruleEngineOutput;
    private String llmNarration;
    private String status;

    public static CoachingEventLogEntry of(
            String sessionCode, long sequenceNumber, String requestedAt, String respondedAt,
            String ruleEngineOutput, String llmNarration, String status
    ) {
        CoachingEventLogEntry entry = new CoachingEventLogEntry();
        entry.setSessionCode(sessionCode);
        entry.setSequenceNumber(sequenceNumber);
        entry.setRequestedAt(requestedAt);
        entry.setRespondedAt(respondedAt);
        entry.setRuleEngineOutput(ruleEngineOutput);
        entry.setLlmNarration(llmNarration);
        entry.setStatus(status);
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
    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(String respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getRuleEngineOutput() {
        return ruleEngineOutput;
    }

    public void setRuleEngineOutput(String ruleEngineOutput) {
        this.ruleEngineOutput = ruleEngineOutput;
    }

    public String getLlmNarration() {
        return llmNarration;
    }

    public void setLlmNarration(String llmNarration) {
        this.llmNarration = llmNarration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
