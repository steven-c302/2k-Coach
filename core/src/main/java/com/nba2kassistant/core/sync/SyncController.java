package com.nba2kassistant.core.sync;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Manual triggers for the periodic syncs — useful for the interview demo and for re-seeding locally. */
@RestController
public class SyncController {

    private final NbaTwoKApiSyncService nbaTwoKApiSyncService;
    private final JsonSeedLoader jsonSeedLoader;

    public SyncController(NbaTwoKApiSyncService nbaTwoKApiSyncService, JsonSeedLoader jsonSeedLoader) {
        this.nbaTwoKApiSyncService = nbaTwoKApiSyncService;
        this.jsonSeedLoader = jsonSeedLoader;
    }

    @PostMapping("/api/admin/sync/nba2kapi")
    public ResponseEntity<?> syncNba2kApi() {
        if (!nbaTwoKApiSyncService.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error", "NBA2KAPI_API_KEY is not configured"));
        }
        int count = nbaTwoKApiSyncService.syncAll();
        return ResponseEntity.ok(Map.of("upserted", count, "source", "NBA2KAPI"));
    }

    @PostMapping("/api/admin/sync/seed")
    public ResponseEntity<?> syncSeed() {
        int count = jsonSeedLoader.loadAndUpsert();
        return ResponseEntity.ok(Map.of("upserted", count, "source", "LOCAL_SCRAPER"));
    }
}
