package com.therighthandapp.autobads.events;

import com.therighthandapp.autobads.integration.dto.SrsHandoffDto;
import java.time.LocalDateTime;

/**
 * Event published when the complete Software Requirements Specification is generated.
 * This is the primary handoff event to AgentMesh for code generation.
 */
public record SrsGeneratedEvent(
    Long ideaId,
    String projectName,
    SrsHandoffDto srsData,
    LocalDateTime generatedAt,
    String correlationId
) {
    public static SrsGeneratedEvent of(Long ideaId, String projectName, SrsHandoffDto srsData) {
        return new SrsGeneratedEvent(
            ideaId,
            projectName,
            srsData,
            LocalDateTime.now(),
            java.util.UUID.randomUUID().toString()
        );
    }
}
