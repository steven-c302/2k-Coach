package com.nba2kassistant.core.matchup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;

/** Best-effort, same reasoning as CoachingEventLogRepository. */
@Component
public class MatchupHistoryRepository {

    private static final Logger log = LoggerFactory.getLogger(MatchupHistoryRepository.class);
    public static final String TABLE_NAME = "MatchupHistory";
    public static final String NO_SESSION = "STANDALONE";

    private final DynamoDbTable<MatchupHistoryEntry> table;

    public MatchupHistoryRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(MatchupHistoryEntry.class));
    }

    DynamoDbTable<MatchupHistoryEntry> table() {
        return table;
    }

    public void append(MatchupHistoryEntry entry) {
        try {
            table.putItem(entry);
        } catch (Exception e) {
            log.warn("Failed to write MatchupHistory entry for session {} (is LocalStack running?): {}",
                    entry.getSessionCode(), e.getMessage());
        }
    }

    public List<MatchupHistoryEntry> findBySessionCode(String sessionCode) {
        try {
            return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(sessionCode).build()))
                    .items().stream().toList();
        } catch (Exception e) {
            log.warn("Failed to read MatchupHistory for session {}: {}", sessionCode, e.getMessage());
            return List.of();
        }
    }
}
