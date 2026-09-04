package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import com.nba2kassistant.core.player.PlayerAttributes;

final class PlayerFixtures {

    private PlayerFixtures() {
    }

    static Player player(long id, String name, String position, int overall) {
        return player(id, name, position, overall, "CURRENT");
    }

    static Player player(long id, String name, String position, int overall, String eraTag) {
        Player p = new Player();
        p.setId(id);
        p.setName(name);
        p.setPosition(position);
        p.setOverall((short) overall);
        p.setEraTag(eraTag);
        p.setSource("TEST");
        return p;
    }

    static PlayerAttributes attributes(
            int threePt, int midRange, int layup, int dunk,
            int speed, int strength, int postDefense, int perimeterDefense
    ) {
        PlayerAttributes a = new PlayerAttributes();
        a.setThreePt((short) threePt);
        a.setMidRange((short) midRange);
        a.setLayup((short) layup);
        a.setDunk((short) dunk);
        a.setSpeed((short) speed);
        a.setStrength((short) strength);
        a.setPostDefense((short) postDefense);
        a.setPerimeterDefense((short) perimeterDefense);
        return a;
    }
}
