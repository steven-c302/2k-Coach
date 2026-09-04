package com.nba2kassistant.core.vision;

import java.time.Instant;
import java.util.Map;

/**
 * Matches the shape both {@code courtvision-vision}'s OCR pipeline and the
 * web app's manual tap-tracker POST here (plan §6) — one unified event
 * stream regardless of source. Core never distinguishes where an event came
 * from; it only routes {@code payload} into the matching session's live
 * state (see {@link VisionEventService}).
 */
public record VisionEventRequest(
        String sessionId,
        VisionEventType type,
        Map<String, Object> payload,
        Instant capturedAt
) {
}
