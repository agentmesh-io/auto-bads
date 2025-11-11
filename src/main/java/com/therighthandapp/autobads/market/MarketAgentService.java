package com.therighthandapp.autobads.market;

import com.therighthandapp.autobads.core.domain.MarketAnalysisResult;
import com.therighthandapp.autobads.core.events.IdeaIngestedEvent;
import com.therighthandapp.autobads.core.events.MarketAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Market Agent Service - Autonomous market intelligence and viability assessment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketAgentService {

    private final SwotAnalysisAgent swotAgent;
    private final PestelAnalysisAgent pestelAgent;
    private final CompetitiveIntelligenceAgent competitiveAgent;
    private final PmfAssessmentAgent pmfAgent;
    private final ApplicationEventPublisher eventPublisher;

    @ApplicationModuleListener
    public void onIdeaIngested(IdeaIngestedEvent event) {
        log.info("Market Agent received IdeaIngestedEvent for idea: {}", event.getIdeaId());

        // Conduct comprehensive market analysis
        MarketAnalysisResult result = analyzeMarket(
                event.getIdeaId().toString(),
                event.getStructuredProblemStatement(),
                event.getBusinessHypothesis()
        );

        // Publish completion event
        MarketAnalysisCompletedEvent completedEvent = MarketAnalysisCompletedEvent.builder()
                .ideaId(UUID.fromString(event.getIdeaId()))
                .analysisResult(result)
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(completedEvent);
        log.info("Market analysis completed for idea: {}", event.getIdeaId());
    }

    private MarketAnalysisResult analyzeMarket(String ideaId, String problemStatement, String hypothesis) {
        log.info("Starting comprehensive market analysis for idea: {}", ideaId);

        // Parallel execution of analysis components
        MarketAnalysisResult.SwotAnalysis swot = swotAgent.performSwotAnalysis(problemStatement);
        MarketAnalysisResult.PestelAnalysis pestel = pestelAgent.performPestelAnalysis(problemStatement);
        MarketAnalysisResult.CompetitiveAnalysis competitive = competitiveAgent.analyzeCompetition(problemStatement);
        MarketAnalysisResult.ProductMarketFit pmf = pmfAgent.assessPmf(hypothesis, problemStatement);

        // Calculate market opportunity score
        double marketScore = calculateMarketOpportunityScore(swot, pestel, competitive, pmf);

        return MarketAnalysisResult.builder()
                .ideaId(ideaId)
                .swot(swot)
                .pestel(pestel)
                .competitive(competitive)
                .pmfAssessment(pmf)
                .opportunities(competitiveAgent.identifyOpportunities(swot, competitive))
                .marketOpportunityScore(marketScore)
                .build();
    }

    private double calculateMarketOpportunityScore(
            MarketAnalysisResult.SwotAnalysis swot,
            MarketAnalysisResult.PestelAnalysis pestel,
            MarketAnalysisResult.CompetitiveAnalysis competitive,
            MarketAnalysisResult.ProductMarketFit pmf) {

        // Weighted calculation of market opportunity
        double pmfWeight = 0.40; // 40% rule is primary indicator
        double competitiveWeight = 0.35;
        double swotWeight = 0.15;
        double trendWeight = 0.10;

        double pmfScore = pmf.getFortyPercentRule();
        double competitiveScore = 100 - (competitive.getCompetitors().size() * 5.0); // More competitors = lower score
        double swotScore = (swot.getOpportunities().size() * 10.0) - (swot.getThreats().size() * 8.0);
        double trendScore = competitive.getMarketTrendVelocity() * 20; // Normalize to 0-100

        double totalScore = (pmfScore * pmfWeight) +
                           (Math.max(0, competitiveScore) * competitiveWeight) +
                           (Math.max(0, Math.min(100, swotScore)) * swotWeight) +
                           (Math.max(0, Math.min(100, trendScore)) * trendWeight);

        return Math.max(0, Math.min(100, totalScore));
    }
}

