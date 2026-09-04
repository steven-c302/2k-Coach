package com.nba2kassistant.core.coaching;

import com.nba2kassistant.core.matchup.MatchupAnalysisService;
import com.nba2kassistant.core.matchup.dto.MatchupAnalysisResponse;
import com.nba2kassistant.core.matchup.dto.MatchupAnalyzeRequest;
import com.nba2kassistant.core.matchup.dto.MismatchResponse;
import com.nba2kassistant.core.session.SessionActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Implements the plan's async/versioned LLM narration literally (§5):
 * mismatches are computed synchronously (fast, deterministic — see
 * MatchupAnalysisService), then the slow LLM call runs on a dedicated
 * executor. When it returns, the request is discarded rather than broadcast
 * if the session has moved to a newer version in the meantime — a stale
 * narration for an old game state would be actively misleading, worse than
 * no narration at all.
 */
@Service
public class CoachingNarrationService {

    private static final Logger log = LoggerFactory.getLogger(CoachingNarrationService.class);

    private final MatchupAnalysisService matchupAnalysisService;
    private final LlmClient llmClient;
    private final CoachingEventLogRepository coachingEventLogRepository;
    private final ExecutorService llmExecutor;

    public CoachingNarrationService(
            MatchupAnalysisService matchupAnalysisService,
            LlmClient llmClient,
            CoachingEventLogRepository coachingEventLogRepository,
            @Qualifier("llmExecutor") ExecutorService llmExecutor
    ) {
        this.matchupAnalysisService = matchupAnalysisService;
        this.llmClient = llmClient;
        this.coachingEventLogRepository = coachingEventLogRepository;
        this.llmExecutor = llmExecutor;
    }

    public void requestNarration(
            String sessionCode, long requestVersion, List<Long> teamAPlayerIds, List<Long> teamBPlayerIds, SessionActor actor
    ) {
        Instant requestedAt = Instant.now();
        MatchupAnalysisResponse analysis = matchupAnalysisService.analyze(
                new MatchupAnalyzeRequest(teamAPlayerIds, teamBPlayerIds, sessionCode));

        CompletableFuture
                .supplyAsync(() -> llmClient.narrate(analysis.mismatches()), llmExecutor)
                .thenAccept(narration -> onNarrationReady(sessionCode, requestVersion, requestedAt, analysis, narration, actor))
                .exceptionally(ex -> {
                    log.error("Coaching narration failed for session {} requestVersion={}", sessionCode, requestVersion, ex);
                    return null;
                });
    }

    private void onNarrationReady(
            String sessionCode, long requestVersion, Instant requestedAt,
            MatchupAnalysisResponse analysis, String narration, SessionActor actor
    ) {
        long currentVersion = actor.currentState().version();
        boolean stale = currentVersion != requestVersion;
        String status = stale ? CoachingEventLogEntry.DISCARDED_STALE : CoachingEventLogEntry.DELIVERED;

        coachingEventLogRepository.save(CoachingEventLogEntry.of(
                sessionCode, requestVersion, requestedAt.toString(), Instant.now().toString(),
                summarize(analysis.mismatches()), narration, status
        ));

        if (stale) {
            log.info("Discarded stale coaching narration for session {}: requestVersion={} currentVersion={}",
                    sessionCode, requestVersion, currentVersion);
            return;
        }
        actor.submit(state -> state.withNarration(narration));
    }

    private static String summarize(List<MismatchResponse> mismatches) {
        return mismatches.isEmpty()
                ? "none"
                : mismatches.stream().map(m -> m.category() + ":" + m.favoredTeam()).collect(Collectors.joining(", "));
    }
}
