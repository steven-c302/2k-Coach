package com.nba2kassistant.core.badge;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerBadgeRepository extends JpaRepository<PlayerBadge, PlayerBadge.Id> {
}
