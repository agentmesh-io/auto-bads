package com.therighthandapp.autobads.ingestion;

import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.core.domain.Status;
import com.therighthandapp.autobads.core.events.IdeaIngestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Idea Ingestion Service - Transforms unstructured ideas into structured problem statements
 */
@Service
public class IdeaIngestionService {
    
    private static final Logger log = LoggerFactory.getLogger(IdeaIngestionService.class);

    private final BusinessIdeaRepository repository;
    private final SemanticTranslationAgent semanticAgent;
    private final ApplicationEventPublisher eventPublisher;
    
    public IdeaIngestionService(BusinessIdeaRepository repository, 
                                SemanticTranslationAgent semanticAgent,
                                ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.semanticAgent = semanticAgent;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID ingestIdea(String rawIdea) {
        // Validate input
        if (rawIdea == null || rawIdea.isBlank()) {
            throw new IllegalArgumentException("Idea cannot be null or empty");
        }
        
        log.info("Ingesting new business idea");

        // Call LLM BEFORE saving to ensure we have complete data
        // This prevents partial state if LLM call fails
        String structuredProblem = semanticAgent.translateToStructuredProblem(rawIdea);
        String businessHypothesis = semanticAgent.generateBusinessHypothesis(structuredProblem);

        // Create entity with all required data
        BusinessIdea idea = BusinessIdea.builder()
                .rawIdea(rawIdea)
                .structuredProblemStatement(structuredProblem)
                .status(Status.ANALYZING)
                .build();

        idea = repository.save(idea);
        log.info("Business idea saved with ID: {}", idea.getId());

        log.info("Idea structured successfully for ID: {}", idea.getId());

        // Publish event for downstream agents
        IdeaIngestedEvent event = IdeaIngestedEvent.builder()
                .ideaId(idea.getId().toString())
                .structuredProblemStatement(structuredProblem)
                .businessHypothesis(businessHypothesis)
                .timestamp(Instant.now())
                .build();

        eventPublisher.publishEvent(event);
        log.info("IdeaIngestedEvent published for ID: {}", idea.getId());

        return idea.getId();
    }

    public BusinessIdea getIdea(UUID ideaId) {
        return repository.findById(ideaId)
                .orElseThrow(() -> new IllegalArgumentException("Idea not found: " + ideaId));
    }
}

