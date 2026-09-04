package com.nba2kassistant.core.coaching;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.nba2kassistant.core.matchup.dto.MismatchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Real narration via the Anthropic Messages API. Effort LOW: this is a short,
 * low-stakes text-formatting task (turn a mismatch list into one coaching
 * sentence), not the kind of long-horizon reasoning that benefits from
 * higher effort — see the plan's cost-tuning guidance.
 */
@Component
class AnthropicLlmClient {

    private static final String SYSTEM_PROMPT = """
            You are a live NBA 2K coaching assistant. Given a list of statistical
            mismatches between two teams, respond with ONE short, punchy coaching
            tip (1-2 sentences, plain spoken language, no bullet points, no
            preamble) telling the player what to exploit right now. If there are
            no mismatches, say the matchup is even and to play their game.
            """;

    private final String apiKey;
    private final String model;
    private final AnthropicClient client;

    AnthropicLlmClient(
            @Value("${nba2kassistant.llm.api-key}") String apiKey,
            @Value("${nba2kassistant.llm.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = apiKey.isBlank() ? null : AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    boolean isConfigured() {
        return client != null;
    }

    String narrate(List<MismatchResponse> mismatches) {
        String mismatchSummary = mismatches.isEmpty()
                ? "No mismatches detected."
                : mismatches.stream()
                        .map(m -> "- [%s] favors %s: %s".formatted(m.severity(), m.favoredTeam(), m.evidence()))
                        .collect(Collectors.joining("\n"));

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1024L)
                .system(SYSTEM_PROMPT)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage(mismatchSummary)
                .build();

        Message response = client.messages().create(params);
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .findFirst()
                .map(text -> text.text())
                .orElse("(no narration returned)");
    }
}
