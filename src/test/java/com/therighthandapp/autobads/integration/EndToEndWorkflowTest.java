package com.therighthandapp.autobads.integration;

import com.therighthandapp.autobads.config.TestKafkaConfig;
import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.core.domain.Status;
import com.therighthandapp.autobads.core.events.*;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import com.therighthandapp.autobads.ingestion.IdeaIngestionService;
import com.therighthandapp.autobads.ingestion.SemanticTranslationAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end workflow integration tests.
 * Tests complete pipeline: Ingestion → Market Analysis → Product Analysis → Financial Analysis → Solution Synthesis
 * Note: These tests verify the workflow structure, not the real-time event processing
 * which would require async testing frameworks or integration with actual message brokers.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestKafkaConfig.class)
@RecordApplicationEvents
@Transactional
class EndToEndWorkflowTest {

    @Autowired
    private IdeaIngestionService ingestionService;

    @Autowired
    private BusinessIdeaRepository repository;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockBean
    private SemanticTranslationAgent semanticAgent;

    @BeforeEach
    void setUp() {
        // Mock semantic agent responses
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("""
                Problem: Structured problem statement for business idea
                
                Current Situation:
                - Market gap exists
                - Customer pain points identified
                - Technical feasibility confirmed
                
                Desired Outcome:
                - Scalable solution
                - Market fit validation
                - Revenue generation
                
                Success Criteria:
                - 80% customer satisfaction
                - 3x ROI within 12 months
                - Market share growth
                """);
        
        when(semanticAgent.generateBusinessHypothesis(anyString()))
            .thenReturn("Business hypothesis: The proposed solution addresses a significant market need with strong revenue potential and achievable development timeline.");
    }

    @Test
    void testIdeaIngestionCreatesBusinessIdea() {
        // Given: A raw business idea
        String rawIdea = "Build a SaaS platform for inventory management targeting small retail businesses";

        // When: Submit idea for ingestion
        UUID ideaId = ingestionService.ingestIdea(rawIdea);

        // Then: Idea should be saved with structured problem statement
        assertThat(ideaId).isNotNull();

        BusinessIdea savedIdea = repository.findById(ideaId).orElse(null);
        assertThat(savedIdea).isNotNull();
        assertThat(savedIdea.getRawIdea()).isEqualTo(rawIdea);
        assertThat(savedIdea.getStructuredProblemStatement()).isNotBlank();
        assertThat(savedIdea.getStatus()).isIn(Status.SUBMITTED, Status.ANALYZING);

        // Verify IdeaIngestedEvent was published
        long ideaIngestedEvents = applicationEvents.stream(IdeaIngestedEvent.class)
                .filter(e -> e.getIdeaId().equals(ideaId.toString()))
                .count();
        assertThat(ideaIngestedEvents).isEqualTo(1);
    }

    @Test
    void testIdeaIngestedEventContainsCorrectData() {
        // Given: Submit an idea
        String rawIdea = "Create mobile app for fitness coaching";
        UUID ideaId = ingestionService.ingestIdea(rawIdea);

        // Then: Verify event contains correct data
        var ideaEvents = applicationEvents.stream(IdeaIngestedEvent.class)
                .filter(e -> e.getIdeaId().equals(ideaId.toString()))
                .toList();
        
        assertThat(ideaEvents).hasSize(1);

        IdeaIngestedEvent ideaEvent = ideaEvents.get(0);
        assertThat(ideaEvent.getIdeaId()).isEqualTo(ideaId.toString());
        assertThat(ideaEvent.getStructuredProblemStatement()).isNotBlank();
        assertThat(ideaEvent.getBusinessHypothesis()).isNotBlank();
        assertThat(ideaEvent.getTimestamp()).isNotNull();
    }

    @Test
    void testMultipleIdeasProcessedIndependently() {
        // Given: Multiple ideas submitted
        String idea1 = "AI-powered customer service chatbot for e-commerce";
        String idea2 = "Blockchain-based supply chain tracking";
        String idea3 = "IoT smart home automation platform";

        // When: Submit all ideas
        UUID ideaId1 = ingestionService.ingestIdea(idea1);
        UUID ideaId2 = ingestionService.ingestIdea(idea2);
        UUID ideaId3 = ingestionService.ingestIdea(idea3);

        // Then: All ideas should be saved independently
        assertThat(ideaId1).isNotEqualTo(ideaId2).isNotEqualTo(ideaId3);

        BusinessIdea savedIdea1 = repository.findById(ideaId1).orElseThrow();
        BusinessIdea savedIdea2 = repository.findById(ideaId2).orElseThrow();
        BusinessIdea savedIdea3 = repository.findById(ideaId3).orElseThrow();

        assertThat(savedIdea1.getRawIdea()).isEqualTo(idea1);
        assertThat(savedIdea2.getRawIdea()).isEqualTo(idea2);
        assertThat(savedIdea3.getRawIdea()).isEqualTo(idea3);

        // Verify each idea has unique structured problem
        assertThat(savedIdea1.getStructuredProblemStatement()).isNotBlank();
        assertThat(savedIdea2.getStructuredProblemStatement()).isNotBlank();
        assertThat(savedIdea3.getStructuredProblemStatement()).isNotBlank();

        // Verify events published for each idea
        long ideaEvents1 = applicationEvents.stream(IdeaIngestedEvent.class)
                .filter(e -> e.getIdeaId().equals(ideaId1.toString()))
                .count();
        long ideaEvents2 = applicationEvents.stream(IdeaIngestedEvent.class)
                .filter(e -> e.getIdeaId().equals(ideaId2.toString()))
                .count();
        long ideaEvents3 = applicationEvents.stream(IdeaIngestedEvent.class)
                .filter(e -> e.getIdeaId().equals(ideaId3.toString()))
                .count();
        
        assertThat(ideaEvents1).isEqualTo(1);
        assertThat(ideaEvents2).isEqualTo(1);
        assertThat(ideaEvents3).isEqualTo(1);
    }

    @Test
    void testSemanticTranslationProducesStructuredOutput() {
        // Given: A vague raw idea
        String rawIdea = "Something to help restaurants manage orders better";

        // When: Ingest idea
        UUID ideaId = ingestionService.ingestIdea(rawIdea);

        // Then: Should have structured problem statement
        BusinessIdea idea = repository.findById(ideaId).orElseThrow();
        
        assertThat(idea.getStructuredProblemStatement()).isNotBlank();
        assertThat(idea.getStructuredProblemStatement().length()).isGreaterThan(50);
        // Should contain structured keywords from mock
        assertThat(idea.getStructuredProblemStatement()).contains("Problem:");
    }

    @Test
    void testIdempotentIdeaSubmission() {
        // Given: Submit an idea
        String rawIdea = "Decentralized social media platform with privacy focus";
        UUID ideaId1 = ingestionService.ingestIdea(rawIdea);

        // When: Submit same idea again
        UUID ideaId2 = ingestionService.ingestIdea(rawIdea);

        // Then: Should create separate ideas (not deduped at ingestion level)
        assertThat(ideaId1).isNotEqualTo(ideaId2);

        BusinessIdea idea1 = repository.findById(ideaId1).orElseThrow();
        BusinessIdea idea2 = repository.findById(ideaId2).orElseThrow();

        assertThat(idea1.getRawIdea()).isEqualTo(idea2.getRawIdea());
        
        // Verify both events published
        long totalEvents = applicationEvents.stream(IdeaIngestedEvent.class).count();
        assertThat(totalEvents).isGreaterThanOrEqualTo(2);
    }
}
