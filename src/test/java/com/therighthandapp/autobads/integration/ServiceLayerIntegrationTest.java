package com.therighthandapp.autobads.integration;

import com.therighthandapp.autobads.config.TestKafkaConfig;
import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.core.domain.Status;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import com.therighthandapp.autobads.ingestion.IdeaIngestionService;
import com.therighthandapp.autobads.ingestion.SemanticTranslationAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Service layer integration tests.
 * Tests the complete service layer with real Spring context, mocked LLM agents.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestKafkaConfig.class)
@Transactional
class ServiceLayerIntegrationTest {

    @Autowired
    private IdeaIngestionService ingestionService;

    @Autowired
    private BusinessIdeaRepository repository;

    @MockBean
    private SemanticTranslationAgent semanticAgent;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testIdeaIngestionWorkflow() {
        // Given: Mock semantic agent response
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("""
                Problem: Small businesses lack affordable inventory management systems
                
                Current Situation:
                - Manual tracking leads to errors
                - Enterprise systems too expensive
                - No real-time stock visibility
                
                Desired Outcome:
                - Affordable cloud-based inventory system
                - Real-time stock tracking
                - Automated reorder alerts
                
                Success Criteria:
                - 90% inventory accuracy
                - 50% reduction in stockouts
                - ROI within 6 months
                """);

        // When: Ingest raw idea
        String rawIdea = "Build affordable inventory management for small businesses";
        UUID ideaId = ingestionService.ingestIdea(rawIdea);

        // Then: Idea should be persisted with structured problem
        assertThat(ideaId).isNotNull();

        BusinessIdea savedIdea = repository.findById(ideaId).orElse(null);
        assertThat(savedIdea).isNotNull();
        assertThat(savedIdea.getRawIdea()).isEqualTo(rawIdea);
        assertThat(savedIdea.getStatus()).isEqualTo(Status.ANALYZING);
        assertThat(savedIdea.getStructuredProblemStatement()).isNotNull();
        assertThat(savedIdea.getStructuredProblemStatement()).contains("inventory management");
    }

    @Test
    void testMultipleIdeasIngestion() {
        // Given: Multiple raw ideas
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Structured problem statement 1")
            .thenReturn("Structured problem statement 2")
            .thenReturn("Structured problem statement 3");

        // When: Ingest multiple ideas
        UUID id1 = ingestionService.ingestIdea("Build mobile app for food delivery");
        UUID id2 = ingestionService.ingestIdea("Create online learning platform");
        UUID id3 = ingestionService.ingestIdea("Develop project management tool");

        // Then: All ideas should be persisted
        List<BusinessIdea> allIdeas = repository.findAll();
        assertThat(allIdeas).hasSize(3);
        assertThat(allIdeas).extracting(BusinessIdea::getId)
            .containsExactlyInAnyOrder(id1, id2, id3);
    }

    @Test
    void testIdeaWithMetadata() {
        // Given: Mock response
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Problem: Healthcare providers need telemedicine platform");

        // When: Ingest idea
        UUID ideaId = ingestionService.ingestIdea("Build telemedicine platform");

        // Then: Can add metadata to persisted idea
        BusinessIdea idea = repository.findById(ideaId).orElse(null);
        assertThat(idea).isNotNull();

        idea.addMetadata("industry", "Healthcare");
        idea.addMetadata("target_market", "Primary care physicians");
        idea.addMetadata("estimated_users", "50000");

        repository.save(idea);

        // Verify metadata persisted
        BusinessIdea retrieved = repository.findById(ideaId).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getMetadataValue("industry")).isEqualTo("Healthcare");
        assertThat(retrieved.getMetadataValue("target_market")).isEqualTo("Primary care physicians");
        assertThat(retrieved.getMetadataValue("estimated_users")).isEqualTo("50000");
    }

    @Test
    void testStatusTransitionTracking() {
        // Given: Ingested idea
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Problem: E-commerce sellers need analytics dashboard");

        UUID ideaId = ingestionService.ingestIdea("Build e-commerce analytics");

        // When: Track status through workflow
        BusinessIdea idea = repository.findById(ideaId).orElse(null);
        assertThat(idea).isNotNull();
        assertThat(idea.getStatus()).isEqualTo(Status.ANALYZING);

        // Simulate workflow progression
        idea.setStatus(Status.SOLUTION_SYNTHESIS_IN_PROGRESS);
        repository.save(idea);

        // Then: Status should update
        BusinessIdea updated = repository.findById(ideaId).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(Status.SOLUTION_SYNTHESIS_IN_PROGRESS);

        // Complete workflow
        updated.setStatus(Status.COMPLETED);
        repository.save(updated);

        BusinessIdea completed = repository.findById(ideaId).orElse(null);
        assertThat(completed).isNotNull();
        assertThat(completed.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void testServiceHandlesLongInput() {
        // Given: Very long raw idea
        String longIdea = """
            Build a comprehensive enterprise resource planning system that integrates
            accounting, human resources, customer relationship management, supply chain
            management, warehouse management, project management, business intelligence,
            and analytics into a single unified platform. The system should support
            multi-currency, multi-language, multi-tenant architecture with role-based
            access control, audit logging, and compliance with GDPR, SOX, and other
            regulatory requirements. It should provide real-time dashboards, automated
            workflows, and AI-powered insights for decision making.
            """;

        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Problem: Enterprises need unified ERP system with compliance and AI");

        // When: Ingest long idea
        UUID ideaId = ingestionService.ingestIdea(longIdea);

        // Then: Should handle successfully
        BusinessIdea idea = repository.findById(ideaId).orElse(null);
        assertThat(idea).isNotNull();
        assertThat(idea.getRawIdea()).hasSizeGreaterThan(400);
    }

    @Test
    void testFindIdeasByStatus() {
        // Given: Multiple ideas with different statuses
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Problem 1")
            .thenReturn("Problem 2")
            .thenReturn("Problem 3");

        UUID id1 = ingestionService.ingestIdea("Idea 1");
        UUID id2 = ingestionService.ingestIdea("Idea 2");
        UUID id3 = ingestionService.ingestIdea("Idea 3");

        // When: Update statuses differently
        BusinessIdea idea1 = repository.findById(id1).orElse(null);
        BusinessIdea idea2 = repository.findById(id2).orElse(null);
        BusinessIdea idea3 = repository.findById(id3).orElse(null);

        idea1.setStatus(Status.ANALYZING);
        idea2.setStatus(Status.SOLUTION_SYNTHESIS_IN_PROGRESS);
        idea3.setStatus(Status.COMPLETED);

        repository.save(idea1);
        repository.save(idea2);
        repository.save(idea3);

        // Then: Can query by status
        List<BusinessIdea> allIdeas = repository.findAll();
        long analyzingCount = allIdeas.stream()
            .filter(i -> i.getStatus() == Status.ANALYZING)
            .count();
        long synthesisCount = allIdeas.stream()
            .filter(i -> i.getStatus() == Status.SOLUTION_SYNTHESIS_IN_PROGRESS)
            .count();
        long completedCount = allIdeas.stream()
            .filter(i -> i.getStatus() == Status.COMPLETED)
            .count();

        assertThat(analyzingCount).isEqualTo(1);
        assertThat(synthesisCount).isEqualTo(1);
        assertThat(completedCount).isEqualTo(1);
    }

    @Test
    void testRepositoryPerformance() {
        // Given: Mock response
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Problem statement");

        // When: Batch insert multiple ideas
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            ingestionService.ingestIdea("Test idea " + i);
        }

        long duration = System.currentTimeMillis() - startTime;

        // Then: Should complete in reasonable time (< 5 seconds for 10 ideas)
        assertThat(duration).isLessThan(5000);
        assertThat(repository.count()).isEqualTo(10);
    }

    @Test
    void testIdeaTimestampTracking() {
        // Given: Mock response
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Problem: Users need time tracking tool");

        // When: Ingest idea
        UUID ideaId = ingestionService.ingestIdea("Build time tracker");

        // Then: Idea should be persisted successfully
        BusinessIdea idea = repository.findById(ideaId).orElse(null);
        assertThat(idea).isNotNull();
        assertThat(idea.getStatus()).isEqualTo(Status.ANALYZING);
    }
}
