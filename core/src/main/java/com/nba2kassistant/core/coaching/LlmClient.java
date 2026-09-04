package com.nba2kassistant.core.coaching;

import com.nba2kassistant.core.matchup.dto.MismatchResponse;

import java.util.List;

/** Turns rule-engine output into a short spoken coaching tip (plan §5). Synchronous by design — CoachingNarrationService is what dispatches this onto the dedicated llmExecutor. */
public interface LlmClient {

    String narrate(List<MismatchResponse> mismatches);
}
