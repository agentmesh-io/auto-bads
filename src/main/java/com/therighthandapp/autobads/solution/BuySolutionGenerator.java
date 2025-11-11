package com.therighthandapp.autobads.solution;

import com.therighthandapp.autobads.core.domain.SolutionPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Generator for BUY solution recommendations
 * Analyzes existing solutions, vendors, and integration costs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuySolutionGenerator {
    private final ChatClient chatClient;

    public SolutionPackage generateBuySolution(SynthesisContext context) {
        log.info("Generating BUY solution for idea: {}", context.getIdeaEvent().getIdeaId());
        
        String prompt = String.format("""
                Generate a BUY solution recommendation for this business idea:
                
                Idea: %s
                Market Analysis: Available
                Product Analysis: Available
                Financial Analysis: Available
                
                Recommend:
                1. Existing SaaS/platform solutions
                2. Vendor comparison
                3. Integration requirements
                4. TCO vs Build
                5. Implementation timeline
                """, context.getIdeaEvent().getDescription());
        
        // TODO: In production, use chatClient to parse LLM response
        return SolutionPackage.builder()
                .packageId("buy-solution-" + System.currentTimeMillis())
                .ideaId(context.getIdeaEvent().getIdeaId())
                .type(SolutionPackage.SolutionType.BUY)
                .description("Purchase existing SaaS platform with proven track record in the industry. " +
                             "Pros: Fast deployment, lower upfront cost, proven reliability. " +
                             "Cons: Less customization, vendor lock-in, recurring costs.")
                .architecture(SolutionPackage.ArchitecturalBlueprint.builder()
                    .systemArchitecture("SaaS Platform Integration")
                    .components(Arrays.asList("Vendor API", "Data Sync Layer", "Custom Integrations"))
                    .integrationPoints(Arrays.asList("REST API", "Webhooks", "OAuth 2.0"))
                    .dataFlowDiagram("User -> SaaS Platform -> Internal Systems")
                    .technologyStack(Arrays.asList("Vendor Platform", "Integration Middleware", "API Gateway"))
                    .build())
                .features(Arrays.asList(
                    createFeature("vendor-integration", "Vendor Platform Setup", "MUST_HAVE", 5),
                    createFeature("data-migration", "Data Migration", "MUST_HAVE", 8),
                    createFeature("custom-branding", "Custom Branding", "SHOULD_HAVE", 3)
                ))
                .score(SolutionPackage.SolutionScore.builder()
                    .strategicAlignment(85.0)
                    .technicalFeasibility(90.0)
                    .marketOpportunity(75.0)
                    .resourceCost(40.0)  // Lower is better
                    .weightedTotalScore(82.0)
                    .recommendation("BUY - Fast deployment with lower upfront cost")
                    .build())
                .build();
    }
    
    private SolutionPackage.Feature createFeature(String id, String name, String priority, int effort) {
        return SolutionPackage.Feature.builder()
                .id(id)
                .name(name)
                .description(name + " implementation")
                .priority(priority)
                .acceptance_criteria("Functional and tested")
                .estimatedEffort(effort)
                .build();
    }
}
