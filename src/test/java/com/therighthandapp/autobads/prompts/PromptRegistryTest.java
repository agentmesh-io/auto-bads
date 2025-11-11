package com.therighthandapp.autobads.prompts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PromptRegistry
 * Tests prompt registration, retrieval, and versioning
 */
@DisplayName("PromptRegistry Tests")
class PromptRegistryTest {

    private PromptRegistry promptRegistry;

    @BeforeEach
    void setUp() {
        promptRegistry = new PromptRegistry();
        promptRegistry.initialize();
    }

    @Test
    @DisplayName("Should load all prompts on initialization")
    void shouldLoadAllPromptsOnInitialization() {
        // When
        Collection<PromptTemplate> allPrompts = promptRegistry.getAllPrompts();

        // Then
        assertThat(allPrompts).isNotEmpty();
        assertThat(allPrompts.size()).isGreaterThanOrEqualTo(7); // 7 agents minimum
    }

    @Test
    @DisplayName("Should retrieve prompt by ID")
    void shouldRetrievePromptById() {
        // When
        PromptTemplate prompt = promptRegistry.getPrompt("ideation.problem-statement");

        // Then
        assertThat(prompt).isNotNull();
        assertThat(prompt.getId()).isEqualTo("ideation.problem-statement");
        assertThat(prompt.getVersion()).isNotEmpty();
    }

    @Test
    @DisplayName("Should throw exception for non-existent prompt")
    void shouldThrowExceptionForNonExistentPrompt() {
        // When/Then
        assertThatThrownBy(() -> promptRegistry.getPrompt("non.existent.prompt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should retrieve all prompts by ID prefix")
    void shouldRetrieveAllPromptsByIdPrefix() {
        // When
        Collection<PromptTemplate> allPrompts = promptRegistry.getAllPrompts();

        // Then - Check that we have prompts for different agents
        boolean hasIdeationPrompts = allPrompts.stream().anyMatch(p -> p.getId().startsWith("ideation."));
        boolean hasProductPrompts = allPrompts.stream().anyMatch(p -> p.getId().startsWith("product."));
        boolean hasFinancialPrompts = allPrompts.stream().anyMatch(p -> p.getId().startsWith("financial."));
        
        assertThat(hasIdeationPrompts).isTrue();
        assertThat(hasProductPrompts).isTrue();
        assertThat(hasFinancialPrompts).isTrue();
    }

    @Test
    @DisplayName("All prompts should have valid versions")
    void allPromptsShouldHaveValidVersions() {
        // When
        Collection<PromptTemplate> allPrompts = promptRegistry.getAllPrompts();

        // Then
        assertThat(allPrompts).allMatch(p -> {
            String version = p.getVersion();
            return version != null && version.matches("\\d+\\.\\d+");
        });
    }

    @Test
    @DisplayName("All prompts should have non-empty templates")
    void allPromptsShouldHaveNonEmptyTemplates() {
        // When
        Collection<PromptTemplate> allPrompts = promptRegistry.getAllPrompts();

        // Then
        assertThat(allPrompts).allMatch(p -> 
            p.getTemplate() != null && !p.getTemplate().trim().isEmpty()
        );
    }

    @Test
    @DisplayName("Critical prompts should have few-shot examples")
    void criticalPromptsShouldHaveFewShotExamples() {
        // Given - Critical prompts that require examples
        String[] criticalPromptIds = {
            "ideation.problem-statement",
            "ideation.business-hypothesis",
            "product.innovation-assessment"
        };

        // Then
        for (String promptId : criticalPromptIds) {
            PromptTemplate prompt = promptRegistry.getPrompt(promptId);
            assertThat(prompt.getExamples())
                .as("Prompt %s should have examples", promptId)
                .isNotNull()
                .isNotEmpty();
        }
    }

    @Test
    @DisplayName("All prompts should have descriptions")
    void allPromptsShouldHaveDescriptions() {
        // When
        Collection<PromptTemplate> allPrompts = promptRegistry.getAllPrompts();

        // Then
        assertThat(allPrompts).allMatch(p -> 
            p.getDescription() != null && !p.getDescription().trim().isEmpty()
        );
    }

    @Test
    @DisplayName("Should register custom prompt")
    void shouldRegisterCustomPrompt() {
        // Given
        PromptTemplate customPrompt = PromptTemplate.builder()
                .id("custom.test-prompt")
                .version("1.0")
                .name("Custom Test Prompt")
                .description("A custom test prompt")
                .template("Test {variable}")
                .build();

        // When
        promptRegistry.registerPrompt(customPrompt);

        // Then
        PromptTemplate retrieved = promptRegistry.getPrompt("custom.test-prompt");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo("custom.test-prompt");
    }

    @Test
    @DisplayName("Should update existing prompt")
    void shouldUpdateExistingPrompt() {
        // Given
        PromptTemplate originalPrompt = promptRegistry.getPrompt("ideation.problem-statement");
        String originalVersion = originalPrompt.getVersion();

        PromptTemplate updatedPrompt = PromptTemplate.builder()
                .id("ideation.problem-statement")
                .version("99.0")
                .name("Updated Prompt")
                .description("Updated description")
                .template(originalPrompt.getTemplate())
                .build();

        // When
        promptRegistry.registerPrompt(updatedPrompt);

        // Then
        PromptTemplate retrieved = promptRegistry.getPrompt("ideation.problem-statement");
        assertThat(retrieved.getVersion()).isEqualTo("99.0");
        assertThat(retrieved.getVersion()).isNotEqualTo(originalVersion);
    }
}
