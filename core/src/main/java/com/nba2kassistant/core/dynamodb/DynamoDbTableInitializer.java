package com.nba2kassistant.core.dynamodb;

import com.nba2kassistant.core.coaching.CoachingEventLogEntry;
import com.nba2kassistant.core.coaching.CoachingEventLogRepository;
import com.nba2kassistant.core.matchup.MatchupHistoryEntry;
import com.nba2kassistant.core.matchup.MatchupHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Bootstraps the two DynamoDB tables on startup if missing — mirrors
 * SyncScheduler's "create what's needed, once, idempotently" pattern from
 * Milestone 1 rather than requiring a separate manual provisioning step.
 */
@Component
public class DynamoDbTableInitializer {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTableInitializer.class);

    private final DynamoDbEnhancedClient enhancedClient;

    public DynamoDbTableInitializer(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createTablesIfMissing() {
        ensureTableExists(
                enhancedClient.table(CoachingEventLogRepository.TABLE_NAME, TableSchema.fromBean(CoachingEventLogEntry.class)),
                CoachingEventLogRepository.TABLE_NAME
        );
        ensureTableExists(
                enhancedClient.table(MatchupHistoryRepository.TABLE_NAME, TableSchema.fromBean(MatchupHistoryEntry.class)),
                MatchupHistoryRepository.TABLE_NAME
        );
    }

    private void ensureTableExists(DynamoDbTable<?> table, String name) {
        try {
            table.describeTable();
            log.info("DynamoDB table {} already exists", name);
        } catch (ResourceNotFoundException e) {
            log.info("DynamoDB table {} not found, creating it", name);
            table.createTable(CreateTableEnhancedRequest.builder()
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build());
        } catch (Exception e) {
            log.warn("Could not verify/create DynamoDB table {} (is LocalStack running?): {}", name, e.getMessage());
        }
    }
}
