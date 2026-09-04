package com.nba2kassistant.core.coaching;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A dedicated, bounded pool for the slow LLM I/O — deliberately NOT the
 * default ForkJoinPool and NOT a session actor's executor (plan §5.3), so a
 * slow or backed-up LLM call can never starve the low-latency game-state
 * path (join/ready/version-bump mutations keep processing on their own
 * per-session executors regardless of how many narration requests are
 * in flight).
 */
@Configuration
public class LlmExecutorConfig {

    private static final int POOL_SIZE = 4;

    @Bean(name = "llmExecutor", destroyMethod = "shutdown")
    public ExecutorService llmExecutor() {
        return Executors.newFixedThreadPool(POOL_SIZE);
    }
}
