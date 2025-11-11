package com.therighthandapp.autobads.prompts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CLI tool for testing and validating prompts
 * 
 * Usage:
 * mvn spring-boot:run -Dspring-boot.run.arguments="--test-prompts=true"
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "test-prompts", havingValue = "true")
@RequiredArgsConstructor
public class PromptTestingCli implements CommandLineRunner {

    private final PromptRegistry promptRegistry;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Prompt Registry Testing ===");
        
        // List all prompts
        log.info("Total prompts loaded: {}", promptRegistry.getAllPrompts().size());
        
        // Test each prompt
        for (PromptTemplate prompt : promptRegistry.getAllPrompts()) {
            log.info("\n--- Testing Prompt: {} v{} ---", prompt.getName(), prompt.getVersion());
            log.info("ID: {}", prompt.getId());
            log.info("Description: {}", prompt.getDescription());
            log.info("Required Variables: {}", prompt.getRequiredVariables());
            
            if (prompt.getExamples() != null && !prompt.getExamples().isEmpty()) {
                log.info("Few-shot Examples: {} examples", prompt.getExamples().size());
                
                // Show first example
                PromptTemplate.FewShotExample firstExample = prompt.getExamples().get(0);
                log.info("  Example 1 Input: {}", 
                    firstExample.getInput().substring(0, Math.min(100, firstExample.getInput().length())) + "...");
            }
            
            if (prompt.getValidation() != null) {
                log.info("Validation Rules:");
                PromptTemplate.ValidationRules rules = prompt.getValidation();
                if (rules.getMinLength() != null) {
                    log.info("  Min Length: {}", rules.getMinLength());
                }
                if (rules.getMaxLength() != null) {
                    log.info("  Max Length: {}", rules.getMaxLength());
                }
                if (rules.getMustContain() != null) {
                    log.info("  Must Contain: {}", rules.getMustContain());
                }
                if (rules.getOutputFormat() != null) {
                    log.info("  Output Format: {}", rules.getOutputFormat());
                }
            }
            
            // Test template fill with sample data
            try {
                String samplePrompt = prompt.getPromptWithExamples();
                log.info("Prompt template length: {} chars", samplePrompt.length());
            } catch (Exception e) {
                log.error("Error generating prompt: {}", e.getMessage());
            }
        }
        
        // Summary by agent
        log.info("\n=== Prompts by Agent ===");
        String[] agents = {"Ideation", "Requirements", "Product", "Financial", "Market", "Solution", "Integration"};
        for (String agent : agents) {
            var agentPrompts = promptRegistry.getPromptsByAgent(agent);
            log.info("{}: {} prompts", agent, agentPrompts.size());
            for (PromptTemplate p : agentPrompts) {
                log.info("  - {} ({})", p.getName(), p.getId());
            }
        }
        
        log.info("\n=== Testing Complete ===");
        
        // Exit application
        System.exit(0);
    }
}
