package com.nba2kassistant.core.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class NbaTwoKApiClient {

    private static final Logger log = LoggerFactory.getLogger(NbaTwoKApiClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public NbaTwoKApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${nba2kassistant.nba2kapi.base-url}") String baseUrl,
            @Value("${nba2kassistant.nba2kapi.api-key}") String apiKey
    ) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * One bulk request per sync (see nba2kapi docs: bulk export "costs only 1 API
     * request" regardless of result size) — this is why sync is a periodic batch
     * job against Postgres, not a hot-path call per §1 of the plan.
     */
    public List<NbaTwoKApiPlayerDto> fetchAllPlayers(String teamType) {
        if (!isConfigured()) {
            throw new IllegalStateException("NBA2KAPI_API_KEY is not configured");
        }
        log.info("Fetching nba2kapi bulk player export (teamType={})", teamType);
        NbaTwoKApiBulkResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/players/bulk")
                        .queryParam("teamType", teamType)
                        .build())
                .header("X-API-Key", apiKey)
                .retrieve()
                .body(NbaTwoKApiBulkResponse.class);

        if (response == null || !response.success()) {
            throw new IllegalStateException("nba2kapi bulk export returned an unsuccessful response");
        }
        return response.data();
    }
}
