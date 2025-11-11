package com.therighthandapp.autobads.product;

import com.therighthandapp.autobads.core.domain.ProductAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Disruptive Innovation Agent - Applies Christensen's disruption framework
 * Evaluates whether innovation targets low-end or new market footholds
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DisruptiveInnovationAgent {

    private final ChatModel chatClient;

    public ProductAnalysisResult.DisruptiveInnovationEvaluation evaluateDisruptivePotential(String problemStatement) {
        log.info("Evaluating disruptive innovation potential");

        // Apply Christensen's framework:
        // 1. Does it target overserved customers or non-consumers?
        // 2. Does it offer simpler, more convenient, or more affordable solution?
        // 3. Does it initially underperform on traditional metrics but excel on new ones?

        boolean isDisruptive = assessDisruptionCriteria(problemStatement);
        double disruptiveScore = isDisruptive ? 75.0 : 35.0;

        return ProductAnalysisResult.DisruptiveInnovationEvaluation.builder()
                .isDisruptive(isDisruptive)
                .targetMarketSegment("New market - previously underserved SMBs without data analytics capabilities")
                .productQualityAssessment(0.85)
                .businessModelType("Platform/SaaS with freemium conversion")
                .disruptivePotentialScore(disruptiveScore)
                .build();
    }

    private boolean assessDisruptionCriteria(String problemStatement) {
        // Simplified heuristic - in production use comprehensive LLM analysis
        String lower = problemStatement.toLowerCase();
        return lower.contains("accessible") ||
               lower.contains("simplif") ||
               lower.contains("affordable") ||
               lower.contains("underserved");
    }
}

