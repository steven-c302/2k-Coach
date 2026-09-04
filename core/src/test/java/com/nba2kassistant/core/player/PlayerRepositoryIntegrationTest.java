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
 * one method never leak into the next.
 *
 * <p>{@code @SpringBootTest} boots the full application context, which means
 * {@code SyncScheduler}'s startup bootstrap genuinely runs too — an empty
 * `players` table gets seeded with all 1082 real local-scraper players
 * before any test method executes. Every query here therefore filters on a
 * nonsense, per-test {@code eraTag} that no real seed data uses, so
 * assertions are isolated from that real data rather than colliding with it
 * (an earlier version of this test asserted exact result sets without that
 * isolation and failed in CI once real seed data was present alongside it).
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
        save("Test PG", "PG", 90, "ZZTEST_POSITION");
        save("Test SG", "SG", 90, "ZZTEST_POSITION");
        save("Test PG Low", "PG", 60, "ZZTEST_POSITION");

        List<PlayerResponse> result = playerService.search(new PlayerSearchRequest("PG", 80, 99, "ZZTEST_POSITION", null));

        assertThat(result).extracting(PlayerResponse::name).containsExactly("Test PG");
    }

    @Test
    void filtersByEraTagAgainstRealPostgres() {
        save("Isolated Era Player", "PG", 90, "ZZTEST_ERA_ONE");
        save("Other Era Player", "PG", 90, "ZZTEST_ERA_TWO");

        List<PlayerResponse> result = playerService.search(new PlayerSearchRequest(null, null, null, "ZZTEST_ERA_ONE", null));

        assertThat(result).extracting(PlayerResponse::name).containsExactly("Isolated Era Player");
    }

    @Test
    void nameSearchIsCaseInsensitiveSubstringMatch() {
        save("Zzzephyr Jordan", "SF", 99, "ZZTEST_NAME");
        save("Zzzephyr James", "SF", 99, "ZZTEST_NAME");

        List<PlayerResponse> result = playerService.search(
                new PlayerSearchRequest(null, null, null, "ZZTEST_NAME", "zzzephyr jordan"));

        assertThat(result).extracting(PlayerResponse::name).containsExactly("Zzzephyr Jordan");
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
