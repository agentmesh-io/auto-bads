package com.therighthandapp.autobads.health;

import com.therighthandapp.autobads.ingestion.SemanticTranslationAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for OpenAI/LLM service connectivity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LLMHealthIndicator implements HealthIndicator {

    private final SemanticTranslationAgent semanticAgent;

    @Override
    public Health health() {
        try {
            // Simple test call to verify LLM connectivity
            String testResult = semanticAgent.translateToStructuredProblem("health check");
            
            if (testResult != null && !testResult.isBlank()) {
                return Health.up()
                        .withDetail("llm_service", "operational")
                        .withDetail("provider", "openai")
                        .build();
            } else {
                return Health.down()
                        .withDetail("llm_service", "degraded")
                        .withDetail("error", "empty_response")
                        .build();
            }
        } catch (Exception e) {
            log.error("LLM health check failed", e);
            return Health.down()
                    .withDetail("llm_service", "unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
