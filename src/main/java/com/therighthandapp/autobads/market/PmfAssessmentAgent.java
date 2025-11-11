package com.therighthandapp.autobads.market;

import com.therighthandapp.autobads.core.domain.MarketAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Product-Market Fit Assessment Agent
 * Uses the 40% rule: PMF achieved when 40%+ of users say they'd be "very disappointed" without the product
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PmfAssessmentAgent {

    private final ChatModel chatClient;

    public MarketAnalysisResult.ProductMarketFit assessPmf(String hypothesis, String problemStatement) {
        log.info("Assessing Product-Market Fit potential");

        // In production, this would:
        // 1. Use LLM to simulate user sentiment based on problem statement
        // 2. Analyze market research data if available
        // 3. Apply statistical models for PMF prediction

        // Mock PMF indicators based on problem severity and market conditions
        double fortyPercentRule = calculatePmfScore(problemStatement);
        String assessment = fortyPercentRule >= 40.0 ?
                "Strong PMF potential - problem resonates with target market" :
                "PMF unclear - requires validation and iteration";

        return MarketAnalysisResult.ProductMarketFit.builder()
                .customerRetentionRate(0.75) // 75% estimated retention
                .churnRate(0.25) // 25% estimated churn
                .npsScore(42.0) // Net Promoter Score
                .fortyPercentRule(fortyPercentRule)
                .assessment(assessment)
                .build();
    }

    private double calculatePmfScore(String problemStatement) {
        // Simplified heuristic - in production use ML model or actual survey data
        // Factors: urgency keywords, pain point clarity, market size indicators

        double baseScore = 35.0;

        if (problemStatement.toLowerCase().contains("critical") ||
            problemStatement.toLowerCase().contains("urgent")) {
            baseScore += 15.0;
        }

        if (problemStatement.toLowerCase().contains("waste") ||
            problemStatement.toLowerCase().contains("inefficient")) {
            baseScore += 10.0;
        }

        if (problemStatement.toLowerCase().contains("expensive") ||
            problemStatement.toLowerCase().contains("costly")) {
            baseScore += 8.0;
        }

        return Math.min(100.0, baseScore);
    }
}
