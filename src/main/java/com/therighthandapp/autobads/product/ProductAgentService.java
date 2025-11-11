package com.therighthandapp.autobads.product;

import com.therighthandapp.autobads.core.domain.ProductAnalysisResult;
import com.therighthandapp.autobads.core.events.IdeaIngestedEvent;
import com.therighthandapp.autobads.core.events.ProductAnalysisCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Product Agent Service - Evaluates product innovation potential and scalability
 */
@Service
public class ProductAgentService {

    private static final Logger log = LoggerFactory.getLogger(ProductAgentService.class);

    private final InnovationAssessmentAgent innovationAgent;
    private final DesignThinkingAgent designThinkingAgent;
    private final DisruptiveInnovationAgent disruptiveAgent;
    private final TrizAgent trizAgent;
    private final ScalabilityAgent scalabilityAgent;
    private final ApplicationEventPublisher eventPublisher;

    public ProductAgentService(InnovationAssessmentAgent innovationAgent,
                               DesignThinkingAgent designThinkingAgent,
                               DisruptiveInnovationAgent disruptiveAgent,
                               TrizAgent trizAgent,
                               ScalabilityAgent scalabilityAgent,
                               ApplicationEventPublisher eventPublisher) {
        this.innovationAgent = innovationAgent;
        this.designThinkingAgent = designThinkingAgent;
        this.disruptiveAgent = disruptiveAgent;
        this.trizAgent = trizAgent;
        this.scalabilityAgent = scalabilityAgent;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    public void onIdeaIngested(IdeaIngestedEvent event) {
        log.info("Product Agent received IdeaIngestedEvent for idea: {}", event.getIdeaId());

        ProductAnalysisResult result = analyzeProduct(
                event.getIdeaId(),
                event.getStructuredProblemStatement(),
                event.getBusinessHypothesis()
        );

        ProductAnalysisCompletedEvent completedEvent = ProductAnalysisCompletedEvent.builder()
                .ideaId(UUID.fromString(event.getIdeaId()))
                .analysisResult(result)
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(completedEvent);
        log.info("Product analysis completed for idea: {}", event.getIdeaId());
    }

    private ProductAnalysisResult analyzeProduct(String ideaId, String problemStatement, String hypothesis) {
        log.info("Starting comprehensive product analysis for idea: {}", ideaId);

        // Apply multiple innovation frameworks
        ProductAnalysisResult.InnovationAssessment innovation =
                innovationAgent.assessInnovation(problemStatement);

        ProductAnalysisResult.DesignThinkingInsights designThinking =
                designThinkingAgent.applyDesignThinking(problemStatement);

        ProductAnalysisResult.DisruptiveInnovationEvaluation disruptive =
                disruptiveAgent.evaluateDisruptivePotential(problemStatement);

        ProductAnalysisResult.TrizAnalysis triz =
                trizAgent.applyTriz(problemStatement);

        ProductAnalysisResult.ScalabilityAssessment scalability =
                scalabilityAgent.assessScalability(problemStatement);

        ProductAnalysisResult.UserEngagementPotential engagement =
                scalabilityAgent.assessUserEngagement(problemStatement);

        // Calculate overall product score
        double productScore = calculateProductScore(innovation, scalability, disruptive, engagement);

        return ProductAnalysisResult.builder()
                .ideaId(ideaId)
                .innovation(innovation)
                .scalability(scalability)
                .userEngagement(engagement)
                .designThinking(designThinking)
                .disruptiveEval(disruptive)
                .trizAnalysis(triz)
                .productScore(productScore)
                .build();
    }

    private double calculateProductScore(
            ProductAnalysisResult.InnovationAssessment innovation,
            ProductAnalysisResult.ScalabilityAssessment scalability,
            ProductAnalysisResult.DisruptiveInnovationEvaluation disruptive,
            ProductAnalysisResult.UserEngagementPotential engagement) {

        // Weighted product score calculation
        return (innovation.getInnovationLevel() * 0.30) +
               (scalability.getScalabilityScore() * 0.25) +
               (disruptive.getDisruptivePotentialScore() * 0.20) +
               (engagement.getEngagementScore() * 0.25);
    }
}
/**
 * Product Analysis Module - Phase III: Product Innovation and Scalability Assessment
 *
 * This module implements the Product Agent from the Discovery Triad.
 *
 * Key responsibilities:
 * - Innovation assessment and classification
 * - Design Thinking framework application
 * - Disruptive Innovation evaluation (Christensen's framework)
 * - TRIZ methodology for systematic innovation
 * - Scalability and user engagement analysis
 */
