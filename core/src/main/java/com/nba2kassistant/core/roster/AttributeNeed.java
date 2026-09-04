package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.PlayerAttributes;

/**
 * Scores how well a candidate's attributes cover an anchor's weaknesses: for
 * each of the 8 tracked attributes, a candidate scores points only where the
 * anchor is below {@link #WEAK_THRESHOLD} AND the candidate is above
 * {@link #STRONG_THRESHOLD} there — filling a real gap, not just being good
 * everywhere. Missing attributes (most seeded players, absent an nba2kapi
 * sync) contribute 0 rather than throwing, since attribute data is optional
 * by design (see JsonSeedLoader).
 */
final class AttributeNeed {

    private static final short WEAK_THRESHOLD = 75;
    private static final short STRONG_THRESHOLD = 85;

    private AttributeNeed() {
    }

    static double complementScore(PlayerAttributes anchor, PlayerAttributes candidate) {
        if (anchor == null || candidate == null) {
            return 0;
        }
        return need(anchor.getThreePt(), candidate.getThreePt())
                + need(anchor.getMidRange(), candidate.getMidRange())
                + need(anchor.getLayup(), candidate.getLayup())
                + need(anchor.getDunk(), candidate.getDunk())
                + need(anchor.getSpeed(), candidate.getSpeed())
                + need(anchor.getStrength(), candidate.getStrength())
                + need(anchor.getPostDefense(), candidate.getPostDefense())
                + need(anchor.getPerimeterDefense(), candidate.getPerimeterDefense());
    }

    private static double need(Short anchorValue, Short candidateValue) {
        if (anchorValue == null || candidateValue == null) {
            return 0;
        }
        if (anchorValue >= WEAK_THRESHOLD || candidateValue < STRONG_THRESHOLD) {
            return 0;
        }
        return candidateValue - anchorValue;
    }
}
