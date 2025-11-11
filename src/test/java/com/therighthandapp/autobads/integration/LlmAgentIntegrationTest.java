package com.therighthandapp.autobads.integration;

import com.therighthandapp.autobads.ingestion.SemanticTranslationAgent;
import com.therighthandapp.autobads.prompts.PromptRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * Integration tests for LLM-powered agents.
 * Tests SemanticTranslationAgent with mocked ChatModel to verify LLM interaction patterns.
 */
class LlmAgentIntegrationTest {

    private ChatModel mockChatModel;
    private PromptRegistry promptRegistry;
    private SemanticTranslationAgent semanticAgent;

    @BeforeEach
    void setUp() {
        mockChatModel = mock(ChatModel.class);
        promptRegistry = new PromptRegistry();
        promptRegistry.initialize();
        semanticAgent = new SemanticTranslationAgent(mockChatModel, promptRegistry);
    }

    @Test
    void testTranslateToStructuredProblem() {
        // Given: Raw unstructured idea
        String rawIdea = "I want to build an app that helps people find local restaurants";
        
        // Mock LLM response with structured problem statement
        String mockResponse = """
            Problem: Users struggle to discover local restaurants that match their preferences
            
            Current Situation:
            - Limited discovery beyond major chains
            - Reviews scattered across multiple platforms
            - No personalized recommendations
            
            Desired Outcome:
            - Unified restaurant discovery platform
            - Personalized recommendations based on preferences
            - Real-time availability and wait times
            
            Success Criteria:
            - 80% user satisfaction with recommendations
            - 50% reduction in time to find suitable restaurant
            - 30% increase in local restaurant discovery
            """;
        
        ChatResponse chatResponse = createMockChatResponse(mockResponse);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // When: Translate to structured problem
        String result = semanticAgent.translateToStructuredProblem(rawIdea);
        
        // Then: Should return structured problem statement
        assertThat(result).isNotNull();
        assertThat(result).contains("Problem:");
        assertThat(result).contains("Current Situation:");
        assertThat(result).contains("Desired Outcome:");
        assertThat(result).contains("Success Criteria:");
        assertThat(result).contains("restaurant");
        
        // Verify LLM was called
        verify(mockChatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testGenerateBusinessHypothesis() {
        // Given: Structured problem statement
        String structuredProblem = """
            Problem: Users struggle to discover local restaurants that match their preferences
            Current Situation: Limited discovery beyond major chains
            Desired Outcome: Unified restaurant discovery platform
            Success Criteria: 80% user satisfaction with recommendations
            """;
        
        // Mock LLM response with business hypothesis
        String mockResponse = """
            Hypothesis: If we provide personalized restaurant recommendations based on user preferences and dining history,
            then users will discover 3x more local restaurants and increase their dining frequency by 40%.
            
            Assumptions:
            1. Users have consistent dining preferences
            2. Local restaurants want increased visibility
            3. Real-time data is accessible
            
            Validation Method:
            1. A/B test with 1000 users over 30 days
            2. Track discovery rate and dining frequency
            3. Measure user satisfaction via NPS
            
            Success Metrics:
            - 3x increase in local restaurant discovery
            - 40% increase in dining frequency
            - NPS score > 50
            """;
        
        ChatResponse chatResponse = createMockChatResponse(mockResponse);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // When: Generate business hypothesis
        String result = semanticAgent.generateBusinessHypothesis(structuredProblem);
        
        // Then: Should return testable hypothesis
        assertThat(result).isNotNull();
        assertThat(result).contains("Hypothesis:");
        assertThat(result).contains("Assumptions:");
        assertThat(result).contains("Validation Method:");
        assertThat(result).contains("Success Metrics:");
        
        // Verify LLM was called
        verify(mockChatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testPromptContainsExamples() {
        // Given: Raw idea
        String rawIdea = "Build a project management tool";
        
        // Mock LLM to capture the prompt
        String mockResponse = "Problem: Teams lack effective project coordination tools";
        ChatResponse chatResponse = createMockChatResponse(mockResponse);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // When: Call translation
        semanticAgent.translateToStructuredProblem(rawIdea);
        
        // Then: Verify prompt structure was used
        verify(mockChatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testMultipleAgentCalls() {
        // Given: Multiple ideas
        String idea1 = "Build a fitness tracking app";
        String idea2 = "Create an online learning platform";
        
        String response1 = "Problem: Users lack comprehensive fitness tracking across activities";
        String response2 = "Problem: Learners need personalized, adaptive learning paths";
        
        when(mockChatModel.call(any(Prompt.class)))
            .thenReturn(createMockChatResponse(response1))
            .thenReturn(createMockChatResponse(response2));
        
        // When: Translate multiple ideas
        String result1 = semanticAgent.translateToStructuredProblem(idea1);
        String result2 = semanticAgent.translateToStructuredProblem(idea2);
        
        // Then: Should handle multiple calls correctly
        assertThat(result1).contains("fitness tracking");
        assertThat(result2).contains("learning paths");
        
        verify(mockChatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void testAgentHandlesLongResponse() {
        // Given: Raw idea
        String rawIdea = "Build comprehensive business analytics platform";
        
        // Mock long detailed response
        String mockResponse = """
            Problem: Organizations lack unified business intelligence and analytics capabilities
            
            Current Situation:
            - Data scattered across multiple systems and departments
            - Manual reporting processes consuming significant time
            - Limited real-time insights for decision making
            - Analytics expertise concentrated in specialized teams
            - High cost of enterprise BI tools
            
            Desired Outcome:
            - Unified analytics platform accessible to all stakeholders
            - Real-time dashboards and automated reporting
            - Self-service analytics for business users
            - Integration with existing data sources
            - Cost-effective solution for SMBs
            
            Success Criteria:
            - 70% reduction in manual reporting time
            - 90% of business users can create basic reports
            - 100% of key metrics available in real-time
            - ROI positive within 12 months
            - 85% user adoption rate within 6 months
            
            Constraints:
            - Must integrate with legacy systems
            - GDPR and data privacy compliance required
            - Support for 1000+ concurrent users
            - 99.9% uptime SLA
            """;
        
        ChatResponse chatResponse = createMockChatResponse(mockResponse);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // When: Translate
        String result = semanticAgent.translateToStructuredProblem(rawIdea);
        
        // Then: Should handle long response
        assertThat(result).hasSizeGreaterThan(500);
        assertThat(result).contains("Problem:");
        assertThat(result).contains("Success Criteria:");
        assertThat(result).contains("Constraints:");
    }

    @Test
    void testAgentWithMinimalInput() {
        // Given: Very brief idea
        String rawIdea = "Mobile game";
        
        // Mock minimal but valid response
        String mockResponse = """
            Problem: Casual gamers need engaging mobile entertainment
            
            Current Situation:
            - Limited quality free-to-play options
            - High monetization pressure in existing games
            
            Desired Outcome:
            - Engaging casual mobile game
            - Fair monetization model
            
            Success Criteria:
            - 1M downloads in first 6 months
            - 4.5+ app store rating
            """;
        
        ChatResponse chatResponse = createMockChatResponse(mockResponse);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // When: Translate minimal input
        String result = semanticAgent.translateToStructuredProblem(rawIdea);
        
        // Then: Should still produce valid output
        assertThat(result).isNotNull();
        assertThat(result).contains("Problem:");
        assertThat(result).contains("Success Criteria:");
    }

    @Test
    void testPromptRegistryIntegration() {
        // Given: Agent uses prompt registry
        String rawIdea = "Build e-commerce platform";
        
        // Verify prompt template exists
        assertThat(promptRegistry.getPrompt("ideation.problem-statement")).isNotNull();
        assertThat(promptRegistry.getPrompt("ideation.business-hypothesis")).isNotNull();
        
        // Mock response
        ChatResponse chatResponse = createMockChatResponse("Problem: Online sellers need integrated platform");
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // When: Use agent
        String result = semanticAgent.translateToStructuredProblem(rawIdea);
        
        // Then: Should successfully use prompt from registry
        assertThat(result).isNotNull();
    }

    @Test
    void testHypothesisValidation() {
        // Given: Structured problem
        String problem = "Problem: Users need better time management tools";
        
        // Mock hypothesis with all required sections
        String mockResponse = """
            Hypothesis: If we provide AI-powered scheduling assistance,
            then users will save 5 hours per week on scheduling tasks.
            
            Assumptions:
            1. Users spend significant time on scheduling
            2. AI can accurately predict optimal meeting times
            
            Validation Method:
            1. Track time saved via user logs
            2. Measure scheduling efficiency
            
            Success Metrics:
            - 5 hours saved per week per user
            - 90% scheduling accuracy
            """;
        
        ChatResponse chatResponse = createMockChatResponse(mockResponse);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // When: Generate hypothesis
        String result = semanticAgent.generateBusinessHypothesis(problem);
        
        // Then: Should contain all required sections
        assertThat(result).contains("Hypothesis:");
        assertThat(result).contains("Assumptions:");
        assertThat(result).contains("Validation Method:");
        assertThat(result).contains("Success Metrics:");
    }

    /**
     * Helper method to create mock ChatResponse
     */
    private ChatResponse createMockChatResponse(String content) {
        AssistantMessage message = new AssistantMessage(content);
        Generation generation = new Generation(message);
        return new ChatResponse(java.util.List.of(generation));
    }
}
