package com.nba2kassistant.core.player;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Postgres, real Flyway migration, real Specification-built query — the
 * plan's testing strategy calls this out explicitly for query-shape-sensitive
 * logic ("don't mock the DB here"). Each test runs inside a rolled-back
 * transaction (Spring's default @Transactional test behavior) so rows from
 * one method never leak into the next, despite the container being shared
 * across the class for speed.
 */
@SpringBootTest
@Testcontainers
@Transactional
class PlayerRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void filtersByPositionAndOverallRangeAgainstRealPostgres() {
        save("Test PG", "PG", 90, "CURRENT");
        save("Test SG", "SG", 90, "CURRENT");
        save("Test PG Low", "PG", 60, "CURRENT");

        List<PlayerResponse> result = playerService.search(new PlayerSearchRequest("PG", 80, 99, null, null));

        assertThat(result).extracting(PlayerResponse::name).containsExactly("Test PG");
    }

    @Test
    void filtersByEraTagAgainstRealPostgres() {
        save("Classic Player", "PG", 90, "1996");
        save("Current Player", "PG", 90, "CURRENT");

        List<PlayerResponse> result = playerService.search(new PlayerSearchRequest(null, null, null, "1996", null));

        assertThat(result).extracting(PlayerResponse::name).containsExactly("Classic Player");
    }

    @Test
    void nameSearchIsCaseInsensitiveSubstringMatch() {
        save("Michael Jordan", "SF", 99, "CURRENT");
        save("LeBron James", "SF", 99, "CURRENT");

        List<PlayerResponse> result = playerService.search(new PlayerSearchRequest(null, null, null, null, "jordan"));

        assertThat(result).extracting(PlayerResponse::name).containsExactly("Michael Jordan");
    }

    private void save(String name, String position, int overall, String eraTag) {
        Player player = new Player();
        player.setName(name);
        player.setPosition(position);
        player.setOverall((short) overall);
        player.setEraTag(eraTag);
        player.setSource("TEST");
        playerRepository.save(player);
    }
}
