package com.therighthandapp.autobads.prompts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PromptTemplate
 * Tests template filling, validation, and few-shot example handling
 */
@DisplayName("PromptTemplate Tests")
class PromptTemplateTest {

    private PromptTemplate.PromptTemplateBuilder baseTemplateBuilder;

    @BeforeEach
    void setUp() {
        baseTemplateBuilder = PromptTemplate.builder()
                .id("test.prompt")
                .version("1.0")
                .name("Test Agent")
                .description("Test prompt template");
    }

    @Test
    @DisplayName("Should fill template with variables correctly")
    void shouldFillTemplateWithVariables() {
        // Given
        PromptTemplate template = baseTemplateBuilder
                .template("Hello {name}, you are {age} years old.")
                .requiredVariables(Arrays.asList("name", "age"))
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");
        variables.put("age", 30);

        // When
        String result = template.fill(variables);

        // Then
        assertThat(result).isEqualTo("Hello John, you are 30 years old.");
    }

    @Test
    @DisplayName("Should throw exception when required variable is missing")
    void shouldThrowExceptionWhenRequiredVariableMissing() {
        // Given
        PromptTemplate template = baseTemplateBuilder
                .template("Hello {name}")
                .requiredVariables(Arrays.asList("name"))
                .build();

        Map<String, Object> variables = new HashMap<>();

        // When/Then
        assertThatThrownBy(() -> template.fill(variables))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required variable: name");
    }

    @Test
    @DisplayName("Should include few-shot examples in prompt")
    void shouldIncludeFewShotExamples() {
        // Given
        PromptTemplate.FewShotExample example1 = PromptTemplate.FewShotExample.builder()
                .input("What is 2+2?")
                .expectedOutput("4")
                .explanation("Basic addition")
                .build();

        PromptTemplate.FewShotExample example2 = PromptTemplate.FewShotExample.builder()
                .input("What is 5*5?")
                .expectedOutput("25")
                .explanation("Basic multiplication")
                .build();

        PromptTemplate template = baseTemplateBuilder
                .template("Calculate: {problem}")
                .examples(Arrays.asList(example1, example2))
                .build();

        // When
        String result = template.getPromptWithExamples();

        // Then
        assertThat(result)
                .contains("### Examples:")
                .contains("Input: What is 2+2?")
                .contains("Output: 4")
                .contains("Input: What is 5*5?")
                .contains("Output: 25");
    }

    @Test
    @DisplayName("Should validate output length correctly")
    void shouldValidateOutputLength() {
        // Given
        PromptTemplate.ValidationRules rules = PromptTemplate.ValidationRules.builder()
                .minLength(10)
                .maxLength(50)
                .build();

        PromptTemplate template = baseTemplateBuilder
                .template("Test")
                .validation(rules)
                .build();

        // When - Too short
        PromptTemplate.ValidationResult tooShort = template.validate("Short");
        
        // Then
        assertThat(tooShort.isValid()).isFalse();
        assertThat(tooShort.getMessage()).contains("too short");

        // When - Valid
        PromptTemplate.ValidationResult valid = template.validate("This is a valid response");
        
        // Then
        assertThat(valid.isValid()).isTrue();

        // When - Too long
        PromptTemplate.ValidationResult tooLong = template.validate("This is a very long response that exceeds the maximum allowed length");
        
        // Then
        assertThat(tooLong.isValid()).isFalse();
        assertThat(tooLong.getMessage()).contains("too long");
    }

    @Test
    @DisplayName("Should validate required keywords")
    void shouldValidateRequiredKeywords() {
        // Given
        PromptTemplate.ValidationRules rules = PromptTemplate.ValidationRules.builder()
                .mustContain(Arrays.asList("innovation", "market"))
                .mustNotContain(Arrays.asList("spam", "invalid"))
                .build();

        PromptTemplate template = baseTemplateBuilder
                .template("Test")
                .validation(rules)
                .build();

        // When - Missing required keyword
        PromptTemplate.ValidationResult missingKeyword = template.validate("This is about innovation only");
        
        // Then
        assertThat(missingKeyword.isValid()).isFalse();
        assertThat(missingKeyword.getMessage()).contains("Missing required content: 'market'");

        // When - Contains forbidden keyword
        PromptTemplate.ValidationResult forbidden = template.validate("innovation market spam content");
        
        // Then
        assertThat(forbidden.isValid()).isFalse();
        assertThat(forbidden.getMessage()).contains("Contains forbidden content: 'spam'");

        // When - Valid
        PromptTemplate.ValidationResult valid = template.validate("innovation in the market is growing");
        
        // Then
        assertThat(valid.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should handle null validation rules gracefully")
    void shouldHandleNullValidationRules() {
        // Given
        PromptTemplate template = baseTemplateBuilder
                .template("Test")
                .validation(null)
                .build();

        // When
        PromptTemplate.ValidationResult result = template.validate("Any content");

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getMessage()).isEqualTo("No validation rules defined");
    }

    @Test
    @DisplayName("Should build template with all metadata")
    void shouldBuildTemplateWithAllMetadata() {
        // Given/When
        PromptTemplate template = PromptTemplate.builder()
                .id("ideation.problem-statement")
                .version("2.0")
                .name("Ideation Agent")
                .description("Transforms raw ideas into structured problem statements")
                .template("Analyze: {rawIdea}")
                .requiredVariables(Arrays.asList("rawIdea"))
                .build();

        // Then
        assertThat(template.getId()).isEqualTo("ideation.problem-statement");
        assertThat(template.getVersion()).isEqualTo("2.0");
        assertThat(template.getName()).isEqualTo("Ideation Agent");
        assertThat(template.getDescription()).isEqualTo("Transforms raw ideas into structured problem statements");
        assertThat(template.getRequiredVariables()).containsExactly("rawIdea");
    }

    @Test
    @DisplayName("Should handle empty examples list")
    void shouldHandleEmptyExamplesList() {
        // Given
        PromptTemplate template = baseTemplateBuilder
                .template("Test template")
                .examples(Arrays.asList())
                .build();

        // When
        String result = template.getPromptWithExamples();

        // Then
        assertThat(result)
                .isEqualTo("Test template")
                .doesNotContain("### Examples:");
    }

    @Test
    @DisplayName("Should handle multiple variable replacements")
    void shouldHandleMultipleVariableReplacements() {
        // Given
        PromptTemplate template = baseTemplateBuilder
                .template("Project: {project}. Budget: ${budget}. Timeline: {timeline} months. Team: {team} people.")
                .requiredVariables(Arrays.asList("project", "budget", "timeline", "team"))
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("project", "E-commerce Platform");
        variables.put("budget", 100000);
        variables.put("timeline", 6);
        variables.put("team", 5);

        // When
        String result = template.fill(variables);

        // Then
        assertThat(result)
                .contains("E-commerce Platform")
                .contains("100000")
                .contains("6 months")
                .contains("5 people");
    }
}
