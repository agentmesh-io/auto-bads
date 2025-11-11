package com.therighthandapp.autobads.financial;

import com.therighthandapp.autobads.core.domain.FinancialAnalysisResult;
import com.therighthandapp.autobads.core.events.IdeaIngestedEvent;
import com.therighthandapp.autobads.core.events.FinancialAnalysisCompletedEvent;
import com.therighthandapp.autobads.core.events.MarketAnalysisCompletedEvent;
import com.therighthandapp.autobads.core.events.ProductAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analytical Agent Service - Hybrid LLM/DL financial analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticalAgentService {

    private final TcoCalculationAgent tcoAgent;
    private final HybridForecastingEngine forecastingEngine;
    private final RiskAssessmentAgent riskAgent;
    private final XaiExplainabilityService xaiService;
    private final ApplicationEventPublisher eventPublisher;

    // Store analysis context from other agents
    private final ConcurrentHashMap<String, AnalysisContext> contextMap = new ConcurrentHashMap<>();

    @ApplicationModuleListener
    public void onIdeaIngested(IdeaIngestedEvent event) {
        log.info("Analytical Agent storing idea context: {}", event.getIdeaId());
        contextMap.put(event.getIdeaId().toString(), new AnalysisContext(event));
    }

    @ApplicationModuleListener
    public void onMarketAnalysisCompleted(MarketAnalysisCompletedEvent event) {
        log.info("Analytical Agent received market analysis for: {}", event.getIdeaId());
        contextMap.computeIfPresent(event.getIdeaId().toString(), (k, ctx) -> {
            ctx.setMarketAnalysis(event.getAnalysisResult());
            return ctx;
        });
    }

    @ApplicationModuleListener
    public void onProductAnalysisCompleted(ProductAnalysisCompletedEvent event) {
        log.info("Analytical Agent received product analysis for: {}", event.getIdeaId());
        contextMap.computeIfPresent(event.getIdeaId().toString(), (k, ctx) -> {
            ctx.setProductAnalysis(event.getAnalysisResult());
            checkAndPerformFinancialAnalysis(event.getIdeaId().toString(), ctx);
            return ctx;
        });
    }

    private void checkAndPerformFinancialAnalysis(String ideaId, AnalysisContext context) {
        if (context.isReadyForFinancialAnalysis()) {
            log.info("All prerequisites met, starting financial analysis for: {}", ideaId);

            FinancialAnalysisResult result = analyzeFinancials(
                    ideaId,
                    context.getIdeaEvent().getStructuredProblemStatement(),
                    context
            );

            FinancialAnalysisCompletedEvent completedEvent = FinancialAnalysisCompletedEvent.builder()
                    .ideaId(UUID.fromString(context.getIdeaEvent().getIdeaId()))
                    .analysisResult(result)
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishEvent(completedEvent);
            log.info("Financial analysis completed for idea: {}", ideaId);
        }
    }

    private FinancialAnalysisResult analyzeFinancials(
            String ideaId,
            String problemStatement,
            AnalysisContext context) {

        log.info("Starting hybrid LLM/DL financial analysis for idea: {}", ideaId);

        // Calculate TCO using LLM
        String industry = context.getIdeaEvent().getIndustry();
        FinancialAnalysisResult.TotalCostOfOwnership tco =
                tcoAgent.calculateTCO(problemStatement, industry);

        // Forecast using hybrid DL/LLM approach
        FinancialAnalysisResult.FinancialForecast forecast =
                forecastingEngine.generateForecast(problemStatement, context, tco);

        // Assess risks
        FinancialAnalysisResult.RiskAssessment risks =
                riskAgent.assessRisks(problemStatement, context.getMarketAnalysis(), context.getProductAnalysis());

        // Generate XAI explanations
        var xaiExplanations = xaiService.generateExplanations(forecast, risks, context);

        // Calculate overall financial viability score
        double viabilityScore = calculateViabilityScore(tco, forecast, risks);

        return FinancialAnalysisResult.builder()
                .ideaId(ideaId)
                .tco(tco)
                .forecast(forecast)
                .riskAssessment(risks)
                .xaiExplanations(xaiExplanations)
                .financialViabilityScore(viabilityScore)
                .build();
    }

    private double calculateViabilityScore(
            FinancialAnalysisResult.TotalCostOfOwnership tco,
            FinancialAnalysisResult.FinancialForecast forecast,
            FinancialAnalysisResult.RiskAssessment risks) {

        double roiScore = Math.min(100, forecast.getRoi() * 2); // Normalize ROI
        double npvScore = forecast.getPredictedNpv().doubleValue() > 0 ? 70 : 30;
        double riskScore = 100 - risks.getOverallRiskScore(); // Invert risk

        return (roiScore * 0.40) + (npvScore * 0.30) + (riskScore * 0.30);
    }
}

