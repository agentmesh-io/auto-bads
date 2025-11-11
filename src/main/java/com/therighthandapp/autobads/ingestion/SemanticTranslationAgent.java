package com.therighthandapp.autobads.ingestion;

import com.therighthandapp.autobads.prompts.PromptRegistry;
import com.therighthandapp.autobads.prompts.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

/**
 * Semantic Translation Agent - Uses LLM to transform unstructured ideas into structured problems
 * Acts as the "secretary" proficient in requirements elicitation
 * Now uses centralized prompt registry with versioning and validation
 */
@Component
public class SemanticTranslationAgent {
    
    private static final Logger log = LoggerFactory.getLogger(SemanticTranslationAgent.class);

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    
    public SemanticTranslationAgent(ChatModel chatModel, PromptRegistry promptRegistry) {
        this.chatModel = chatModel;
        this.promptRegistry = promptRegistry;
    }

    public String translateToStructuredProblem(String rawIdea) {
        log.info("Translating unstructured idea to structured problem statement");
        
        // Get enhanced prompt from registry
        PromptTemplate promptTemplate = promptRegistry.getPrompt("ideation.problem-statement");
        
        // Add few-shot examples and fill variables
        String promptWithExamples = promptTemplate.getPromptWithExamples()
            .replace("{rawIdea}", rawIdea);
        
        // Call LLM
        String response = chatModel.call(new Prompt(promptWithExamples))
            .getResult()
            .getOutput()
            .getContent();
        
        // Validate output
        PromptTemplate.ValidationResult validation = promptTemplate.validate(response);
        if (!validation.isValid()) {
            log.warn("Response validation failed: {}", validation.getMessage());
            // Could retry with improved prompt or use fallback
        }
        
        log.debug("Structured problem: {}", response);
        return response;
    }

    public String generateBusinessHypothesis(String structuredProblem) {
        log.info("Generating testable business hypothesis");
        
        // Get enhanced prompt from registry
        PromptTemplate promptTemplate = promptRegistry.getPrompt("ideation.business-hypothesis");
        
        // Fill template with variables
        String promptWithExamples = promptTemplate.getPromptWithExamples()
            .replace("{problemStatement}", structuredProblem);
        
        // Call LLM
        String response = chatModel.call(new Prompt(promptWithExamples))
            .getResult()
            .getOutput()
            .getContent();
        
        // Validate output
        PromptTemplate.ValidationResult validation = promptTemplate.validate(response);
        if (!validation.isValid()) {
            log.warn("Hypothesis validation failed: {}", validation.getMessage());
        }
        
        log.debug("Business hypothesis: {}", response);
        return response;
    }
}

