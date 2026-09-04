package com.nba2kassistant.core.matchup;

import java.util.List;

public record PlayerSnapshot(
        Long id,
        String name,
        String position,
        PlayerAttributesSnapshot attributes,
        List<BadgeSnapshot> badges
) {
}
