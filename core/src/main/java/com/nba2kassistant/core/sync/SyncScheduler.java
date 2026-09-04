package com.nba2kassistant.core.sync;

import com.nba2kassistant.core.player.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bootstraps data on first startup (empty table) and refreshes nba2kapi data daily
 * thereafter. The bulk endpoint costs one API request per sync, well inside the
 * 100/hr key limit, so a fixed daily cadence needs no backoff/retry logic yet.
 */
@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final PlayerRepository playerRepository;
    private final NbaTwoKApiSyncService nbaTwoKApiSyncService;
    private final JsonSeedLoader jsonSeedLoader;

    public SyncScheduler(
            PlayerRepository playerRepository,
            NbaTwoKApiSyncService nbaTwoKApiSyncService,
            JsonSeedLoader jsonSeedLoader
    ) {
        this.playerRepository = playerRepository;
        this.nbaTwoKApiSyncService = nbaTwoKApiSyncService;
        this.jsonSeedLoader = jsonSeedLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIfEmpty() {
        if (playerRepository.count() > 0) {
            return;
        }
        if (nbaTwoKApiSyncService.isConfigured()) {
            log.info("players table is empty; bootstrapping from nba2kapi");
            nbaTwoKApiSyncService.syncAll();
        } else {
            log.info("players table is empty and NBA2KAPI_API_KEY is unset; bootstrapping from local seed file");
            jsonSeedLoader.loadAndUpsert();
        }
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void dailyRefresh() {
        if (!nbaTwoKApiSyncService.isConfigured()) {
            return;
        }
        nbaTwoKApiSyncService.syncAll();
    }
}
