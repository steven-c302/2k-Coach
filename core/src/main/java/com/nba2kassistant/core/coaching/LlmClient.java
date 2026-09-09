package com.nba2kassistant.core.coaching;

/** Turns rule-engine output (plus live game state, when there is any) into a short spoken coaching tip (plan §5). Synchronous by design — CoachingNarrationService is what dispatches this onto the dedicated llmExecutor. */
public interface LlmClient {

    String narrate(NarrationContext context);
}
