package com.nba2kassistant.core.session;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Durable mirror of the live {@link SessionState} an actor holds in memory
 * (see SessionRegistry) — persisted on every actor-processed mutation so a
 * restart doesn't lose a session mid-game (plan §2).
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String state = SessionStatus.LOBBY.name();

    private String hostClientId;

    private String guestClientId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Long version = 0L;
}
