package com.nba2kassistant.core.coaching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;

/**
 * Writes are best-effort (plan §2: this is an audit trail, not
 * request-critical state) — a DynamoDB/LocalStack outage never fails the
 * narration flow, only its log entry, and is logged as a warning rather than
 * propagated.
 */
@Component
public class CoachingEventLogRepository {

    private static final Logger log = LoggerFactory.getLogger(CoachingEventLogRepository.class);
    public static final String TABLE_NAME = "CoachingEventLog";

    private final DynamoDbTable<CoachingEventLogEntry> table;

    public CoachingEventLogRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(CoachingEventLogEntry.class));
    }

    DynamoDbTable<CoachingEventLogEntry> table() {
        return table;
    }

    public void save(CoachingEventLogEntry entry) {
        try {
            table.putItem(entry);
        } catch (Exception e) {
            log.warn("Failed to write CoachingEventLog entry for session {} (is LocalStack running?): {}",
                    entry.getSessionCode(), e.getMessage());
        }
    }

    public List<CoachingEventLogEntry> findBySessionCode(String sessionCode) {
        try {
            return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(sessionCode).build()))
                    .items().stream().toList();
        } catch (Exception e) {
            log.warn("Failed to read CoachingEventLog for session {}: {}", sessionCode, e.getMessage());
            return List.of();
        }
    }
}
