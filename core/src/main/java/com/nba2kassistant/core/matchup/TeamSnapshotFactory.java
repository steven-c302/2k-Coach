package com.nba2kassistant.core.matchup;

import com.nba2kassistant.core.badge.PlayerBadge;
import com.nba2kassistant.core.badge.PlayerBadgeRepository;
import com.nba2kassistant.core.player.Player;
import com.nba2kassistant.core.player.PlayerAttributes;
import com.nba2kassistant.core.player.PlayerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Detaches JPA entities into the pure {@link TeamSnapshot} the rules engine reads. */
@Component
public class TeamSnapshotFactory {

    private final PlayerRepository playerRepository;
    private final PlayerBadgeRepository playerBadgeRepository;

    public TeamSnapshotFactory(PlayerRepository playerRepository, PlayerBadgeRepository playerBadgeRepository) {
        this.playerRepository = playerRepository;
        this.playerBadgeRepository = playerBadgeRepository;
    }

    public TeamSnapshot build(String label, List<Long> playerIds) {
        List<Player> players = playerRepository.findAllById(playerIds);
        if (players.size() != playerIds.size()) {
            throw new MatchupAnalysisException("One or more player ids not found for " + label);
        }

        Map<Long, List<PlayerBadge>> badgesByPlayerId = playerBadgeRepository.findByIdPlayerIdIn(playerIds).stream()
                .collect(Collectors.groupingBy(pb -> pb.getId().getPlayerId()));

        List<PlayerSnapshot> snapshots = players.stream()
                .map(player -> toSnapshot(player, badgesByPlayerId.getOrDefault(player.getId(), List.of())))
                .toList();

        return new TeamSnapshot(label, snapshots);
    }

    private PlayerSnapshot toSnapshot(Player player, List<PlayerBadge> badges) {
        PlayerAttributesSnapshot attributesSnapshot = toAttributesSnapshot(player.getAttributes());
        List<BadgeSnapshot> badgeSnapshots = badges.stream()
                .map(pb -> new BadgeSnapshot(pb.getBadge().getSlug(), pb.getBadge().getCategory(), pb.getTier()))
                .toList();
        return new PlayerSnapshot(player.getId(), player.getName(), player.getPosition(), attributesSnapshot, badgeSnapshots);
    }

    private PlayerAttributesSnapshot toAttributesSnapshot(PlayerAttributes attributes) {
        if (attributes == null) {
            return null;
        }
        return new PlayerAttributesSnapshot(
                attributes.getThreePt(), attributes.getMidRange(), attributes.getLayup(), attributes.getDunk(),
                attributes.getSpeed(), attributes.getStrength(), attributes.getPostDefense(), attributes.getPerimeterDefense()
        );
    }
}
