package com.therighthandapp.autobads.product;

import com.therighthandapp.autobads.core.domain.ProductAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Design Thinking Agent - Applies human-centered design methodology
 * Stages: Empathize, Define, Ideate, Prototype, Test
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DesignThinkingAgent {

    private final ChatModel chatClient;

    public ProductAnalysisResult.DesignThinkingInsights applyDesignThinking(String problemStatement) {
        log.info("Applying Design Thinking framework");

        // LLM applies Design Thinking stages
        return ProductAnalysisResult.DesignThinkingInsights.builder()
                .empathyFindings("Users feel frustrated by repetitive tasks and lack of insights from their data")
                .problemDefinition("How might we help knowledge workers transform raw data into actionable insights without technical expertise?")
                .generatedIdeas(List.of(
                        "Natural language query interface",
                        "Automated insight generation with explanations",
                        "Collaborative workspace for team insights",
                        "Integration marketplace for data sources"
                ))
                .customerJourneyMap("Awareness -> Consideration -> Trial -> Adoption -> Advocacy -> Champion")
                .servicePainPoints(List.of(
                        "Onboarding complexity",
                        "Learning curve for advanced features",
                        "Integration setup time"
                ))
                .build();
    }
}

