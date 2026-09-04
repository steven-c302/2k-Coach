package com.nba2kassistant.core.player;

import org.springframework.data.jpa.domain.Specification;

/**
 * Composable filter predicates for {@code GET /api/players}. Kept separate from
 * the {@link RosterCriterion}-style pipeline in the team builder (Milestone 2) —
 * this is ad-hoc query filtering, that's roster-building strategy; they look
 * similar but answer different questions.
 */
final class PlayerSpecifications {

    private PlayerSpecifications() {
    }

    static Specification<Player> positionEquals(String position) {
        return (root, query, cb) -> position == null ? null : cb.equal(root.get("position"), position);
    }

    static Specification<Player> overallAtLeast(Integer minOverall) {
        return (root, query, cb) -> minOverall == null ? null : cb.ge(root.get("overall"), minOverall);
    }

    static Specification<Player> overallAtMost(Integer maxOverall) {
        return (root, query, cb) -> maxOverall == null ? null : cb.le(root.get("overall"), maxOverall);
    }

    static Specification<Player> eraTagEquals(String eraTag) {
        return (root, query, cb) -> eraTag == null ? null : cb.equal(root.get("eraTag"), eraTag);
    }

    static Specification<Player> nameContainsIgnoreCase(String nameQuery) {
        return (root, query, cb) -> nameQuery == null ? null
                : cb.like(cb.lower(root.get("name")), "%" + nameQuery.toLowerCase() + "%");
    }
}
