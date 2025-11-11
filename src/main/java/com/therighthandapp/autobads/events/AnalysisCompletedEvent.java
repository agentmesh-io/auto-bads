package com.therighthandapp.autobads.events;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when all analyses (SWOT, PESTEL, PMF, TCO) are completed.
 * Triggers the solution synthesis phase.
 */
public record AnalysisCompletedEvent(
    Long ideaId,
    List<String> completedAnalyses,
    boolean allAnalysesSuccessful,
    LocalDateTime completedAt,
    String correlationId
) {
    public static AnalysisCompletedEvent of(Long ideaId, List<String> completedAnalyses, boolean allSuccessful) {
        return new AnalysisCompletedEvent(
            ideaId,
            completedAnalyses,
            allSuccessful,
            LocalDateTime.now(),
            java.util.UUID.randomUUID().toString()
        );
    }
}
