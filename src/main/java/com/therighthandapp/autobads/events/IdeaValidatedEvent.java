package com.therighthandapp.autobads.events;

import java.time.LocalDateTime;

/**
 * Event published when a business idea passes initial validation.
 * Triggers the market and product analysis phase.
 */
public record IdeaValidatedEvent(
    Long ideaId,
    String ideaTitle,
    String description,
    String targetMarket,
    LocalDateTime validatedAt,
    String validatedBy
) {
    public static IdeaValidatedEvent of(Long ideaId, String ideaTitle, String description, String targetMarket) {
        return new IdeaValidatedEvent(
            ideaId,
            ideaTitle,
            description,
            targetMarket,
            LocalDateTime.now(),
            "AUTO-BADS-VALIDATOR"
        );
    }
}
