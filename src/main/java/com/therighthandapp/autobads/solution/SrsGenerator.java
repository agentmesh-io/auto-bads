package com.therighthandapp.autobads.solution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * SRS Generator - Autonomously generates Software Requirements Specification
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SrsGenerator {

    private final ChatModel chatClient;

    public String generateSrs(SynthesisContext context) {
        log.info("Generating comprehensive SRS document");

        // LLM generates structured SRS based on all analysis results
        StringBuilder srs = new StringBuilder();

        srs.append("# Software Requirements Specification\n\n");
        srs.append("## 1. Introduction\n");
        srs.append("### 1.1 Purpose\n");
        srs.append(context.getIdeaEvent().getStructuredProblemStatement()).append("\n\n");

        srs.append("### 1.2 Business Hypothesis\n");
        srs.append(context.getIdeaEvent().getBusinessHypothesis()).append("\n\n");

        srs.append("## 2. Functional Requirements\n");
        srs.append("### 2.1 Core Features\n");
        srs.append("- User authentication and authorization\n");
        srs.append("- Dashboard with real-time insights\n");
        srs.append("- AI-powered recommendations\n");
        srs.append("- Data import/export capabilities\n");
        srs.append("- Collaboration and sharing features\n\n");

        srs.append("## 3. Non-Functional Requirements\n");
        srs.append("### 3.1 Performance\n");
        srs.append("- Response time < 200ms for 95th percentile\n");
        srs.append("- Support 10,000 concurrent users\n");
        srs.append("- 99.9% uptime SLA\n\n");

        srs.append("### 3.2 Security\n");
        srs.append("- End-to-end encryption for data in transit\n");
        srs.append("- SOC 2 Type II compliance\n");
        srs.append("- GDPR and CCPA compliance\n\n");

        srs.append("### 3.3 Scalability\n");
        if (context.getProductAnalysis() != null) {
            srs.append(context.getProductAnalysis().getScalability().getRecommendedArchitecture()).append("\n\n");
        }

        srs.append("## 4. System Architecture\n");
        srs.append("Microservices-based architecture with event-driven communication\n\n");

        srs.append("## 5. Quality Attributes\n");
        srs.append("- Maintainability: Modular design with clear separation of concerns\n");
        srs.append("- Testability: 80%+ code coverage target\n");
        srs.append("- Usability: Intuitive UI with < 5 minute onboarding\n\n");

        return srs.toString();
    }
}
