package com.nba2kassistant.core.coaching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Real client + local fallback, same shape as NbaTwoKApiClient/JsonSeedLoader
 * in Milestone 1: use the configured Anthropic API when a key is present,
 * fall back to deterministic template narration otherwise (including on a
 * transient Anthropic failure) — so the async/versioning pipeline is fully
 * demoable without external dependencies.
 *
 * <p>{@code @Primary} because {@link TemplateNarrationClient} is also an
 * {@link LlmClient} bean in its own right (so it stays independently
 * injectable/testable) — this is the one everything else should actually get.
 */
@Component
@Primary
public class LlmClientSelector implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClientSelector.class);

    private final AnthropicLlmClient anthropicClient;
    private final TemplateNarrationClient templateClient;

    LlmClientSelector(AnthropicLlmClient anthropicClient, TemplateNarrationClient templateClient) {
        this.anthropicClient = anthropicClient;
        this.templateClient = templateClient;
    }

    @Override
    public String narrate(NarrationContext context) {
        if (!anthropicClient.isConfigured()) {
            return templateClient.narrate(context);
        }
        try {
            return anthropicClient.narrate(context);
        } catch (Exception e) {
            log.warn("Anthropic narration call failed, falling back to template narration: {}", e.getMessage());
            return templateClient.narrate(context);
        }
    }
}
