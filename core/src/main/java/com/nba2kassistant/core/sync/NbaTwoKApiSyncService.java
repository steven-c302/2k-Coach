package com.nba2kassistant.core.sync;

import com.nba2kassistant.core.badge.Badge;
import com.nba2kassistant.core.badge.BadgeRepository;
import com.nba2kassistant.core.badge.PlayerBadge;
import com.nba2kassistant.core.badge.PlayerBadgeRepository;
import com.nba2kassistant.core.player.Player;
import com.nba2kassistant.core.player.PlayerAttributes;
import com.nba2kassistant.core.player.PlayerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Syncs the primary data source (nba2kapi) into Postgres on a schedule — see
 * {@link com.nba2kassistant.core.sync.SyncScheduler} — rather than calling it
 * per-request, per the plan's "synced periodically, not hot-pathed" decision.
 */
@Service
public class NbaTwoKApiSyncService {

    private static final Logger log = LoggerFactory.getLogger(NbaTwoKApiSyncService.class);
    static final String SOURCE = "NBA2KAPI";

    /** nba2kapi's own team-type codes -> our eraTag; the specific season/team stays in {@code team} verbatim. */
    private static final Map<String, String> TEAM_TYPE_TO_ERA_TAG = Map.of(
            "curr", "CURRENT",
            "class", "CLASSIC",
            "allt", "ALL_TIME"
    );

    private final NbaTwoKApiClient client;
    private final PlayerRepository playerRepository;
    private final BadgeRepository badgeRepository;
    private final PlayerBadgeRepository playerBadgeRepository;
    private final ObjectMapper objectMapper;

    public NbaTwoKApiSyncService(
            NbaTwoKApiClient client,
            PlayerRepository playerRepository,
            BadgeRepository badgeRepository,
            PlayerBadgeRepository playerBadgeRepository,
            ObjectMapper objectMapper
    ) {
        this.client = client;
        this.playerRepository = playerRepository;
        this.badgeRepository = badgeRepository;
        this.playerBadgeRepository = playerBadgeRepository;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return client.isConfigured();
    }

    /** Syncs all three of nba2kapi's team types in one pass — current rosters, classic (per-season) teams, and all-time teams. */
    @Transactional
    public int syncAll() {
        int upserted = 0;
        for (Map.Entry<String, String> teamType : TEAM_TYPE_TO_ERA_TAG.entrySet()) {
            List<NbaTwoKApiPlayerDto> players = client.fetchAllPlayers(teamType.getKey());
            for (NbaTwoKApiPlayerDto dto : players) {
                upsertPlayer(dto, teamType.getValue());
                upserted++;
            }
        }
        log.info("nba2kapi sync upserted {} players", upserted);
        return upserted;
    }

    private void upsertPlayer(NbaTwoKApiPlayerDto dto, String eraTag) {
        Player player = playerRepository.findBySourceAndExternalId(SOURCE, dto.slug())
                .orElseGet(Player::new);
        player.setSource(SOURCE);
        player.setExternalId(dto.slug());
        player.setName(dto.name());
        player.setTeam(dto.team());
        player.setEraTag(eraTag);
        player.setPositions(dto.positions() == null ? List.of() : dto.positions());
        player.setPosition(dto.positions() == null || dto.positions().isEmpty() ? null : dto.positions().get(0));
        player.setOverall(dto.overall() == null ? 0 : dto.overall().shortValue());
        player.setLastSyncedAt(Instant.now());
        player = playerRepository.save(player);

        PlayerAttributes attributes = player.getAttributes();
        if (attributes == null) {
            attributes = new PlayerAttributes();
            attributes.setPlayer(player);
        }
        AttributeExtractor.apply(attributes, dto.attributes(), objectMapper);
        player.setAttributes(attributes);
        playerRepository.save(player);

        upsertBadges(player, dto.badges());
    }

    private void upsertBadges(Player player, JsonNode badgesNode) {
        if (badgesNode == null || !badgesNode.has("list") || !badgesNode.get("list").isArray()) {
            return;
        }
        for (JsonNode badgeRef : badgesNode.get("list")) {
            String name = textOrNull(badgeRef, "name");
            if (name == null) {
                continue;
            }
            String slug = textOrNull(badgeRef, "slug");
            if (slug == null) {
                slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
            }
            String category = textOrNull(badgeRef, "category");
            Short tier = BadgeTier.parse(badgeRef.get("tier"));
            if (tier == null) {
                continue;
            }

            String finalSlug = slug;
            Badge badge = badgeRepository.findBySlug(finalSlug).orElseGet(() -> {
                Badge b = new Badge();
                b.setSlug(finalSlug);
                return b;
            });
            badge.setName(name);
            badge.setCategory(category);
            Badge savedBadge = badgeRepository.save(badge);

            PlayerBadge.Id id = new PlayerBadge.Id(player.getId(), savedBadge.getId());
            PlayerBadge playerBadge = playerBadgeRepository.findById(id)
                    .orElseGet(() -> new PlayerBadge(player, savedBadge, tier));
            playerBadge.setTier(tier);
            playerBadgeRepository.save(playerBadge);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
