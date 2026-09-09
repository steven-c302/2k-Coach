package com.nba2kassistant.core.coaching;

import com.nba2kassistant.core.matchup.dto.MismatchResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Deterministic, no-network fallback used when no ANTHROPIC_API_KEY is
 * configured (see LlmClientSelector) — real working code, not a stub, since
 * it's what actually runs the async/versioning/staleness demo in an
 * environment without a live key. Highest-severity mismatch first, plus
 * whatever live game state has been observed so far, if any.
 */
@Component
class TemplateNarrationClient implements LlmClient {

    @Override
    public String narrate(NarrationContext context) {
        String base = narrateMismatches(context.mismatches());
        String live = formatLiveState(context.liveGameState());
        return live.isEmpty() ? base : base + " " + live;
    }

    private static String narrateMismatches(List<MismatchResponse> mismatches) {
        if (mismatches.isEmpty()) {
            return "No clear mismatches — this one's even, play your game.";
        }
        return mismatches.stream()
                .sorted(Comparator.comparingInt(m -> severityRank(m.severity())))
                .map(m -> "%s should %s: %s".formatted(m.favoredTeam(), humanize(m.category()), m.evidence()))
                .collect(Collectors.joining(" "));
    }

    private static String formatLiveState(Map<String, Object> state) {
        if (state == null || state.isEmpty()) {
            return "";
        }
        return "Live: " + state.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static int severityRank(String severity) {
        return switch (severity) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            default -> 2;
        };
    }

    private static String humanize(String category) {
        return category.replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
