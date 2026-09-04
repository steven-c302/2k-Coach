package com.nba2kassistant.core.player;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id/slug from the source feed (nba2kapi); null for local-scraper rows. */
    private String externalId;

    @Column(nullable = false)
    private String name;

    private String team;

    private String position;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> positions = List.of();

    @Column(nullable = false)
    private Short overall;

    /** "CURRENT" for present-day rosters, otherwise a classic-team year like "1996". */
    @Column(nullable = false)
    private String eraTag = "CURRENT";

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private Instant lastSyncedAt = Instant.now();

    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PlayerAttributes attributes;
}
