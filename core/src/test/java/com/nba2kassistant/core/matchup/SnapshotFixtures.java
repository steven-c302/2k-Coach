package com.nba2kassistant.core.matchup;

import java.util.List;

final class SnapshotFixtures {

    private SnapshotFixtures() {
    }

    static PlayerSnapshot player(long id, String name, String position) {
        return new PlayerSnapshot(id, name, position, null, List.of());
    }

    static PlayerSnapshot player(long id, String name, String position, PlayerAttributesSnapshot attributes) {
        return new PlayerSnapshot(id, name, position, attributes, List.of());
    }

    static PlayerSnapshot player(long id, String name, String position, PlayerAttributesSnapshot attributes, List<BadgeSnapshot> badges) {
        return new PlayerSnapshot(id, name, position, attributes, badges);
    }

    static PlayerAttributesSnapshot attributes(
            int threePt, int midRange, int layup, int dunk,
            int speed, int strength, int postDefense, int perimeterDefense
    ) {
        return new PlayerAttributesSnapshot(
                (short) threePt, (short) midRange, (short) layup, (short) dunk,
                (short) speed, (short) strength, (short) postDefense, (short) perimeterDefense
        );
    }

    static TeamSnapshot team(String label, PlayerSnapshot... players) {
        return new TeamSnapshot(label, List.of(players));
    }
}
