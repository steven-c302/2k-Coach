package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nba2kassistant.core.roster.PlayerFixtures.attributes;
import static com.nba2kassistant.core.roster.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;

class BuildAroundPlayerCriterionTest {

    private final RosterBuildContext ctx = new RosterBuildContext(5, "CURRENT", null);

    @Test
    void excludesTheAnchorFromTheReturnedPool() {
        Player anchor = player(1, "Anchor", "PG", 90);
        Player other = player(2, "Other", "SG", 80);

        List<Player> result = new BuildAroundPlayerCriterion(anchor).apply(List.of(anchor, other), ctx);

        assertThat(result).containsExactly(other);
    }

    @Test
    void ranksDifferentPositionAboveSamePositionWhenAttributesAreEqual() {
        Player anchor = player(1, "Anchor", "PG", 90);
        Player samePosition = player(2, "SamePos", "PG", 85);
        Player differentPosition = player(3, "DiffPos", "SG", 85);

        List<Player> result = new BuildAroundPlayerCriterion(anchor)
                .apply(List.of(samePosition, differentPosition), ctx);

        assertThat(result).extracting(Player::getName).containsExactly("DiffPos", "SamePos");
    }

    @Test
    void ranksCandidateHigherWhenTheyCoverAnAnchorWeakness() {
        Player anchor = player(1, "Anchor", "PG", 90);
        anchor.setAttributes(attributes(60, 80, 80, 80, 80, 80, 80, 80)); // weak three-point shooting

        Player sharpshooter = player(2, "Sharpshooter", "PG", 85);
        sharpshooter.setAttributes(attributes(95, 80, 80, 80, 80, 80, 80, 80)); // covers the gap

        Player generalist = player(3, "Generalist", "PG", 85);
        generalist.setAttributes(attributes(80, 80, 80, 80, 80, 80, 80, 80)); // no standout attribute

        List<Player> result = new BuildAroundPlayerCriterion(anchor)
                .apply(List.of(generalist, sharpshooter), ctx);

        assertThat(result).extracting(Player::getName).containsExactly("Sharpshooter", "Generalist");
    }
}
