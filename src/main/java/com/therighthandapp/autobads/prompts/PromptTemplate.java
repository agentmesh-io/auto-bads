package com.therighthandapp.autobads.prompts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Prompt template with versioning and metadata
 * Supports few-shot learning examples and validation rules
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {
    
    private String id;
    private String name;
    private String version;
    private String description;
    private String template;
    private List<String> requiredVariables;
    private List<FewShotExample> examples;
    private ValidationRules validation;
    private Map<String, String> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Few-shot learning example
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FewShotExample {
        private String input;
        private String expectedOutput;
        private String explanation;
    }
    
    /**
     * Output validation rules
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationRules {
        private Integer minLength;
        private Integer maxLength;
        private List<String> mustContain;
        private List<String> mustNotContain;
        private String outputFormat; // JSON, YAML, TEXT, MARKDOWN
        private boolean requireStructuredData;
    }
    
    /**
     * Fill template with variables
     */
    public String fill(Map<String, Object> variables) {
        String result = template;
        
        // Validate required variables
        for (String required : requiredVariables) {
            if (!variables.containsKey(required)) {
                throw new IllegalArgumentException("Missing required variable: " + required);
            }
        }
        
        // Replace placeholders
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        
        return result;
    }
    
    /**
     * Get prompt with few-shot examples
     */
    public String getPromptWithExamples() {
        if (examples == null || examples.isEmpty()) {
            return template;
        }
        
        StringBuilder promptWithExamples = new StringBuilder(template);
        promptWithExamples.append("\n\n### Examples:\n\n");
        
        for (int i = 0; i < examples.size(); i++) {
            FewShotExample example = examples.get(i);
            promptWithExamples.append("Example ").append(i + 1).append(":\n");
            promptWithExamples.append("Input: ").append(example.getInput()).append("\n");
            promptWithExamples.append("Output: ").append(example.getExpectedOutput()).append("\n");
            if (example.getExplanation() != null) {
                promptWithExamples.append("Why: ").append(example.getExplanation()).append("\n");
            }
            promptWithExamples.append("\n");
        }
        
        return promptWithExamples.toString();
    }
    
    /**
     * Validate LLM output against rules
     */
    public ValidationResult validate(String output) {
        if (validation == null) {
            return ValidationResult.builder()
                .valid(true)
                .message("No validation rules defined")
                .build();
        }
        
        StringBuilder errors = new StringBuilder();
        
        // Length validation
        if (validation.getMinLength() != null && output.length() < validation.getMinLength()) {
            errors.append(String.format("Output too short (min: %d, got: %d). ", 
                validation.getMinLength(), output.length()));
        }
        
        if (validation.getMaxLength() != null && output.length() > validation.getMaxLength()) {
            errors.append(String.format("Output too long (max: %d, got: %d). ", 
                validation.getMaxLength(), output.length()));
        }
        
        // Content validation
        if (validation.getMustContain() != null) {
            for (String required : validation.getMustContain()) {
                if (!output.contains(required)) {
                    errors.append(String.format("Missing required content: '%s'. ", required));
                }
            }
        }
        
        if (validation.getMustNotContain() != null) {
            for (String forbidden : validation.getMustNotContain()) {
                if (output.contains(forbidden)) {
                    errors.append(String.format("Contains forbidden content: '%s'. ", forbidden));
                }
            }
        }
        
        boolean isValid = errors.length() == 0;
        
        return ValidationResult.builder()
            .valid(isValid)
            .message(isValid ? "Valid" : errors.toString())
            .build();
    }
    
    @Data
    @Builder
    public static class ValidationResult {
        private boolean valid;
        private String message;
    }
}
