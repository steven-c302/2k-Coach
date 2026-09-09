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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Real narration via the Anthropic Messages API. Effort LOW: this is a short,
 * low-stakes text-formatting task (turn a mismatch list + live game state
 * into a couple of coaching sentences), not the kind of long-horizon
 * reasoning that benefits from higher effort — see the plan's cost-tuning
 * guidance.
 */
@Component
class AnthropicLlmClient {

    private static final String SYSTEM_PROMPT = """
            You are a live NBA 2K coaching assistant watching a real-time matchup. You'll be given
            each team's roster, the pre-game statistical mismatches between them, and — once
            available — the live game state (score, clock, or other observed facts).

            Respond with ONE short, punchy coaching tip: 1-2 sentences before tip-off, up to 3 once
            live game state is present. Plain spoken language, no bullet points, no preamble.

            If live game state is present, react to it specifically (a score deficit, foul trouble,
            a cold stretch) rather than just repeating the pre-game mismatches verbatim — that's
            what makes this a "what do I do right now" tip instead of a scouting report. When it
            fits, name one specific bench player from the roster who should check in and why. If
            there's no live game state yet, give the pre-game strategic tip instead. If there are
            no mismatches and no notable live state, say the matchup is even and to play their game.
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

    String narrate(NarrationContext context) {
        String prompt = buildPrompt(context);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1024L)
                .system(SYSTEM_PROMPT)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage(prompt)
                .build();

        Message response = client.messages().create(params);
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .findFirst()
                .map(text -> text.text())
                .orElse("(no narration returned)");
    }

    private static String buildPrompt(NarrationContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Team A roster: ").append(String.join(", ", context.teamARoster())).append("\n");
        prompt.append("Team B roster: ").append(String.join(", ", context.teamBRoster())).append("\n\n");
        prompt.append("Pre-game mismatches:\n").append(mismatchSummary(context.mismatches()));

        Map<String, Object> liveGameState = context.liveGameState();
        if (liveGameState != null && !liveGameState.isEmpty()) {
            prompt.append("\n\nLive game state: ").append(liveGameState.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ")));
        } else {
            prompt.append("\n\nLive game state: none yet (this is the pre-game analysis).");
        }
        return prompt.toString();
    }

    private static String mismatchSummary(List<MismatchResponse> mismatches) {
        return mismatches.isEmpty()
                ? "No mismatches detected."
                : mismatches.stream()
                        .map(m -> "- [%s] favors %s: %s".formatted(m.severity(), m.favoredTeam(), m.evidence()))
                        .collect(Collectors.joining("\n"));
    }
}
