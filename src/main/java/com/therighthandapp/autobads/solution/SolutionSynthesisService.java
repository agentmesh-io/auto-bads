package com.therighthandapp.autobads.solution;

import com.therighthandapp.autobads.core.domain.*;
import com.therighthandapp.autobads.core.events.*;
import com.therighthandapp.autobads.events.SrsGeneratedEvent;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import com.therighthandapp.autobads.integration.dto.SrsHandoffDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Solution Synthesis Service - Orchestrates solution package generation and recommendation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionSynthesisService {

    private final BuildSolutionGenerator buildGenerator;
    private final BuySolutionGenerator buyGenerator;
    private final HybridSolutionGenerator hybridGenerator;
    private final SrsGenerator srsGenerator;
    private final RecommendationEngine recommendationEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessIdeaRepository ideaRepository;
    private final com.therighthandapp.autobads.events.EventPublisher kafkaEventPublisher;

    private final ConcurrentHashMap<UUID, SynthesisContext> contextMap = new ConcurrentHashMap<>();

    @ApplicationModuleListener
    public void onIdeaIngested(IdeaIngestedEvent event) {
        log.info("Solution Synthesis received idea: {}", event.getIdeaId());
        UUID ideaUuid = UUID.fromString(event.getIdeaId());
        contextMap.put(ideaUuid, new SynthesisContext(event));
    }

    @ApplicationModuleListener
    public void onMarketAnalysisCompleted(MarketAnalysisCompletedEvent event) {
        log.info("Solution Synthesis received market analysis: {}", event.getIdeaId());
        contextMap.computeIfPresent(event.getIdeaId(), (k, ctx) -> {
            ctx.setMarketAnalysis(event.getAnalysisResult());
            return ctx;
        });
    }

    @ApplicationModuleListener
    public void onProductAnalysisCompleted(ProductAnalysisCompletedEvent event) {
        log.info("Solution Synthesis received product analysis: {}", event.getIdeaId());
        contextMap.computeIfPresent(event.getIdeaId(), (k, ctx) -> {
            ctx.setProductAnalysis(event.getAnalysisResult());
            return ctx;
        });
    }

    @ApplicationModuleListener
    public void onFinancialAnalysisCompleted(FinancialAnalysisCompletedEvent event) {
        log.info("Solution Synthesis received financial analysis: {}", event.getIdeaId());
        contextMap.computeIfPresent(event.getIdeaId(), (k, ctx) -> {
            ctx.setFinancialAnalysis(event.getAnalysisResult());
            checkAndSynthesizeSolution(event.getIdeaId(), ctx);
            return ctx;
        });
    }

    private void checkAndSynthesizeSolution(UUID ideaId, SynthesisContext context) {
        if (context.isReadyForSynthesis()) {
            log.info("All analyses complete, synthesizing solutions for: {}", ideaId);

            // Update idea status
            updateIdeaStatus(ideaId, Status.SOLUTION_SYNTHESIS_IN_PROGRESS);

            // Generate all solution packages
            List<SolutionPackage> solutions = generateSolutionPackages(ideaId, context);

            // Recommend best solution
            SolutionPackage recommended = recommendationEngine.selectBestSolution(solutions, context);
            String recommendation = recommendationEngine.generateRecommendation(recommended, context);

            // Update idea status to completed
            updateIdeaStatus(ideaId, Status.COMPLETED);

            // Publish completion event
            SolutionRecommendationCompletedEvent event = SolutionRecommendationCompletedEvent.builder()
                    .ideaId(ideaId)
                    .allSolutionPackages(solutions)
                    .recommendedSolution(recommended)
                    .recommendation(recommendation)
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.info("Solution synthesis completed for idea: {}", ideaId);

            // Publish SRS to Kafka for AgentMesh consumption
            publishSrsToKafka(ideaId, recommended, context);
        }
    }

    private void publishSrsToKafka(UUID ideaId, SolutionPackage recommendedSolution, SynthesisContext context) {
        try {
            // Convert SolutionPackage to SrsHandoffDto
            SrsHandoffDto srsHandoff = convertToSrsHandoff(recommendedSolution, context);
            
            String projectName = recommendedSolution.getDescription() != null ? 
                recommendedSolution.getDescription().substring(0, Math.min(50, recommendedSolution.getDescription().length())) : 
                "Project-" + ideaId;
            
            // Create and publish event
            SrsGeneratedEvent srsEvent = SrsGeneratedEvent.of(
                ideaId.hashCode() & Long.MAX_VALUE, // Convert UUID to Long
                projectName,
                srsHandoff
            );
            
            kafkaEventPublisher.publishSrsGenerated(srsEvent);
            log.info("Published SRS to Kafka for AgentMesh: project={}, correlationId={}", 
                projectName, srsEvent.correlationId());
        } catch (Exception e) {
            log.error("Failed to publish SRS to Kafka for idea: {}", ideaId, e);
        }
    }

    private SrsHandoffDto convertToSrsHandoff(SolutionPackage solution, SynthesisContext context) {
        // Create a simplified SRS handoff DTO using builder pattern
        SrsHandoffDto.SoftwareRequirementsSpecification srs = SrsHandoffDto.SoftwareRequirementsSpecification.builder()
            .version("1.0")
            .functionalRequirements(List.of())
            .nonFunctionalRequirements(List.of())
            .architecture(SrsHandoffDto.SystemArchitecture.builder()
                .architectureStyle("MICROSERVICES")
                .components(List.of())
                .integrationPoints(List.of())
                .databaseStrategy("PostgreSQL")
                .build())
            .dependencies(List.of())
            .build();
        
        SrsHandoffDto.FinancialProjections financials = SrsHandoffDto.FinancialProjections.builder()
            .totalCostOfOwnership(200000.0)
            .developmentCost(100000.0)
            .operationalCostPerYear(20000.0)
            .expectedRoi(1.5)
            .breakEvenMonths(24)
            .build();
        
        SrsHandoffDto.RiskAssessment risks = SrsHandoffDto.RiskAssessment.builder()
            .overallRiskLevel("MEDIUM")
            .identifiedRisks(List.of())
            .build();
        
        return SrsHandoffDto.builder()
            .ideaId(UUID.fromString(solution.getIdeaId()))
            .ideaTitle(solution.getDescription())
            .generatedAt(LocalDateTime.now())
            .businessCase("Auto-generated from Auto-BADS")
            .problemStatement(solution.getDescription())
            .strategicAlignment("High")
            .srs(srs)
            .prioritizedBacklog(List.of())
            .technicalConstraints(List.of())
            .qualityAttributes(List.of())
            .financials(financials)
            .riskAssessment(risks)
            .recommendedSolutionType(solution.getType().toString())
            .recommendationScore(solution.getScore() != null ? 85.0 : 0.0)
            .metadata(java.util.Map.of())
            .build();
    }

    private List<SolutionPackage> generateSolutionPackages(UUID ideaId, SynthesisContext context) {
        log.info("Generating Build, Buy, and Hybrid solution packages");

        String ideaIdStr = ideaId.toString();

        // Generate SRS for all solutions
        String srsDocument = srsGenerator.generateSrs(context);

        // Generate three solution alternatives
        SolutionPackage buildSolution = buildGenerator.generateBuildSolution(ideaIdStr, srsDocument, context);
        SolutionPackage buySolution = buyGenerator.generateBuySolution(context);
        SolutionPackage hybridSolution = hybridGenerator.generateHybridSolution(ideaIdStr, srsDocument, context);

        return List.of(buildSolution, buySolution, hybridSolution);
    }

    private void updateIdeaStatus(UUID ideaId, Status status) {
        ideaRepository.findById(ideaId).ifPresent(idea -> {
            idea.setStatus(status);
            ideaRepository.save(idea);
        });
    }
}

