package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.PlayerAttributes;
import org.junit.jupiter.api.Test;

import static com.nba2kassistant.core.roster.PlayerFixtures.attributes;
import static org.assertj.core.api.Assertions.assertThat;

class AttributeNeedTest {

    @Test
    void scoresOnlyDimensionsWhereAnchorIsWeakAndCandidateIsStrong() {
        PlayerAttributes anchor = attributes(60, 90, 90, 90, 90, 90, 90, 90); // weak three-point only
        PlayerAttributes candidate = attributes(95, 95, 90, 90, 90, 90, 90, 90); // strong everywhere

        double score = AttributeNeed.complementScore(anchor, candidate);

        // Only the three-point gap counts: candidate 95 - anchor 60 = 35.
        // midRange doesn't count even though candidate is "strong" there — anchor isn't weak there.
        assertThat(score).isEqualTo(35);
    }

    @Test
    void ignoresGapsWhereCandidateIsNotActuallyStrong() {
        PlayerAttributes anchor = attributes(60, 90, 90, 90, 90, 90, 90, 90);
        PlayerAttributes candidate = attributes(80, 90, 90, 90, 90, 90, 90, 90); // improvement, but below the strong threshold

        double score = AttributeNeed.complementScore(anchor, candidate);

        assertThat(score).isZero();
    }

    @Test
    void returnsZeroWhenEitherSideHasNoAttributes() {
        assertThat(AttributeNeed.complementScore(null, attributes(99, 99, 99, 99, 99, 99, 99, 99))).isZero();
        assertThat(AttributeNeed.complementScore(attributes(50, 50, 50, 50, 50, 50, 50, 50), null)).isZero();
    }
}
