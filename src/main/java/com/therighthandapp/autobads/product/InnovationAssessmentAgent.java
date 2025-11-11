package com.therighthandapp.autobads.product;

import com.therighthandapp.autobads.core.domain.ProductAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Innovation Assessment Agent - Evaluates the innovation level and type
 * Classifies innovation as Incremental, Radical, or Disruptive
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InnovationAssessmentAgent {

    private final ChatModel chatClient;

    public ProductAnalysisResult.InnovationAssessment assessInnovation(String problemStatement) {
        log.info("Assessing innovation level and type");

        // Analyze the innovation level using LLM
        double innovationLevel = calculateInnovationLevel(problemStatement);
        String innovationType = classifyInnovationType(innovationLevel, problemStatement);

        return ProductAnalysisResult.InnovationAssessment.builder()
                .innovationLevel(innovationLevel)
                .innovationType(innovationType)
                .uniqueValuePropositions(List.of(
                        "AI-powered automated analysis without coding",
                        "Real-time collaborative insights platform",
                        "Natural language data exploration",
                        "Predictive analytics for business outcomes"
                ))
                .innovationRisks(List.of(
                        "Technical feasibility of NLP accuracy",
                        "Market adoption rate uncertainty",
                        "Competitive response from established players",
                        "Data privacy and security concerns"
                ))
                .build();
    }

    private double calculateInnovationLevel(String problemStatement) {
        // Simplified scoring - in production use comprehensive LLM analysis
        String lower = problemStatement.toLowerCase();
        double score = 50.0; // Base score

        // Increase score for innovation indicators
        if (lower.contains("ai") || lower.contains("machine learning")) score += 15;
        if (lower.contains("automated") || lower.contains("automation")) score += 10;
        if (lower.contains("real-time") || lower.contains("instant")) score += 8;
        if (lower.contains("predictive") || lower.contains("forecast")) score += 12;
        if (lower.contains("natural language")) score += 10;

        return Math.min(score, 95.0); // Cap at 95
    }

    private String classifyInnovationType(double level, String problemStatement) {
        if (level >= 75) return "Disruptive";
        if (level >= 50) return "Radical";
        return "Incremental";
    }
}

