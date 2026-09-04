package com.nba2kassistant.core.player;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> search(PlayerSearchRequest request) {
        Specification<Player> spec = Specification
                .where(PlayerSpecifications.positionEquals(request.position()))
                .and(PlayerSpecifications.overallAtLeast(request.minOverall()))
                .and(PlayerSpecifications.overallAtMost(request.maxOverall()))
                .and(PlayerSpecifications.eraTagEquals(request.eraTag()))
                .and(PlayerSpecifications.nameContainsIgnoreCase(request.name()));

        return playerRepository.findAll(spec).stream()
                .map(PlayerResponse::from)
                .toList();
    }
}
