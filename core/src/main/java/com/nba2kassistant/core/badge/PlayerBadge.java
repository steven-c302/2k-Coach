package com.nba2kassistant.core.badge;

import com.nba2kassistant.core.player.Player;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "player_badges")
@Getter
@Setter
@NoArgsConstructor
public class PlayerBadge {

    @EmbeddedId
    private Id id = new Id();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("playerId")
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("badgeId")
    @JoinColumn(name = "badge_id")
    private Badge badge;

    @Column(nullable = false)
    private Short tier;

    public PlayerBadge(Player player, Badge badge, Short tier) {
        this.player = player;
        this.badge = badge;
        this.tier = tier;
        this.id = new Id(player.getId(), badge.getId());
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        private Long playerId;
        private Long badgeId;

        public Id(Long playerId, Long badgeId) {
            this.playerId = playerId;
            this.badgeId = badgeId;
        }
    }
}
