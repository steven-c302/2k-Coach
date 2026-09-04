package com.nba2kassistant.core.badge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PlayerBadgeRepository extends JpaRepository<PlayerBadge, PlayerBadge.Id> {

    List<PlayerBadge> findByIdPlayerIdIn(Collection<Long> playerIds);
}
