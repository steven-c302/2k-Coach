package com.nba2kassistant.core.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Credentials resolve via the SDK's default provider chain (AWS_ACCESS_KEY_ID/
 * AWS_SECRET_ACCESS_KEY env vars — see docker-compose.yml's LocalStack values);
 * no credentials provider bean needed. Client construction never fails even
 * without a reachable DynamoDB — connections are only attempted at call time,
 * and every call site (CoachingEventLogRepository, MatchupHistoryRepository)
 * treats failures as best-effort (plan §2: this is audit logging, not
 * request-critical state).
 */
@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(
            @Value("${nba2kassistant.dynamodb.endpoint}") String endpointOverride,
            @Value("${nba2kassistant.dynamodb.region}") String region
    ) {
        var builder = DynamoDbClient.builder().region(Region.of(region));
        if (!endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }
}
