package com.nba2kassistant.core.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    Optional<Player> findBySourceAndExternalId(String source, String externalId);

    Optional<Player> findBySourceAndNameAndTeamAndEraTag(String source, String name, String team, String eraTag);

    @Query("SELECT DISTINCT p.eraTag FROM Player p ORDER BY p.eraTag")
    List<String> findDistinctEraTags();

    @Query("SELECT DISTINCT p.team FROM Player p WHERE p.eraTag = :eraTag AND p.team IS NOT NULL ORDER BY p.team")
    List<String> findDistinctTeamsByEraTag(String eraTag);
}
