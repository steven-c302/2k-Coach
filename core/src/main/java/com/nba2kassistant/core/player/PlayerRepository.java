package com.nba2kassistant.core.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    Optional<Player> findBySourceAndExternalId(String source, String externalId);

    Optional<Player> findBySourceAndNameAndTeamAndEraTag(String source, String name, String team, String eraTag);
}
