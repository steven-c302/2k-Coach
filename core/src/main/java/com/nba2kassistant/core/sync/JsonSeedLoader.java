package com.nba2kassistant.core.sync;

import com.nba2kassistant.core.common.EraTag;
import com.nba2kassistant.core.player.Player;
import com.nba2kassistant.core.player.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * Fallback data source (NBA2K Assistant plan §9): the local scraper only has
 * name/team/position/overall, no attributes/badges, but it works with zero
 * external setup so the project runs out of the box before an nba2kapi key
 * is configured.
 */
@Service
public class JsonSeedLoader {

    private static final Logger log = LoggerFactory.getLogger(JsonSeedLoader.class);
    static final String SOURCE = "LOCAL_SCRAPER";

    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;
    private final Resource seedResource;

    public JsonSeedLoader(
            PlayerRepository playerRepository,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${nba2kassistant.seed.players-json-path}") String seedPath
    ) {
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
        this.seedResource = resourceLoader.getResource(seedPath);
    }

    @Transactional
    public int loadAndUpsert() {
        List<ScraperPlayer> scraped = readScraperFile();
        int upserted = 0;
        for (ScraperPlayer sp : scraped) {
            EraTag.Parsed parsed = EraTag.parse(sp.team());
            Player player = playerRepository
                    .findBySourceAndNameAndTeamAndEraTag(SOURCE, sp.name(), parsed.team(), parsed.eraTag())
                    .orElseGet(Player::new);
            player.setSource(SOURCE);
            player.setName(sp.name());
            player.setTeam(parsed.team());
            player.setEraTag(parsed.eraTag());
            player.setPosition(sp.position());
            player.setPositions(sp.position() == null ? List.of() : List.of(sp.position()));
            player.setOverall(sp.overall());
            player.setLastSyncedAt(Instant.now());
            playerRepository.save(player);
            upserted++;
        }
        log.info("Seed bootstrap upserted {} players from {}", upserted, seedResource);
        return upserted;
    }

    private List<ScraperPlayer> readScraperFile() {
        try (InputStream in = seedResource.getInputStream()) {
            return objectMapper.readValue(in, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ScraperPlayer.class));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read seed file " + seedResource, e);
        }
    }
}
