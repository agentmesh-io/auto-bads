package com.therighthandapp.autobads.core.events;

import com.therighthandapp.autobads.core.domain.BusinessIdea;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when a business idea has been successfully ingested
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaIngestedEvent {
    private String ideaId;
    private String title;
    private String description;
    private String structuredProblemStatement;  // AI-generated structured statement
    private String businessHypothesis;  // AI-generated hypothesis
    private String industry;
    private String targetMarket;
    private Instant timestamp;
    
    public static IdeaIngestedEvent from(BusinessIdea idea) {
        return IdeaIngestedEvent.builder()
                .ideaId(idea.getId().toString())
                .title(idea.getTitle())
                .description(idea.getDescription())
                .structuredProblemStatement(idea.getStructuredProblemStatement())
                .businessHypothesis("Generated from: " + idea.getDescription())
                .industry(idea.getIndustry())
                .targetMarket(idea.getTargetMarket())
                .timestamp(Instant.now())
                .build();
    }
}
