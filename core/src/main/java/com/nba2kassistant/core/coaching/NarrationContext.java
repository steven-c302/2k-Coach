package com.nba2kassistant.core.coaching;

import com.nba2kassistant.core.matchup.dto.MismatchResponse;

import java.util.List;
import java.util.Map;

/**
 * Everything an {@link LlmClient} needs to narrate one matchup: the static pre-game rule-engine
 * output, each roster (so a tip can name a specific bench player), and whatever's been observed
 * of the live game so far (score/clock from OCR or the tap-tracker) — empty before anything's
 * been observed, e.g. the very first "analyze" click at tip-off.
 */
public record NarrationContext(
        List<MismatchResponse> mismatches,
        Map<String, Object> liveGameState,
        List<String> teamARoster,
        List<String> teamBRoster
) {
}
