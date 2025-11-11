package com.therighthandapp.autobads.integration;

import com.therighthandapp.autobads.config.TestKafkaConfig;
import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.core.domain.Status;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for database operations.
 * Tests JPA entities, repositories, and database interactions.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestKafkaConfig.class)
@Transactional
class DatabaseIntegrationTest {

    @Autowired
    private BusinessIdeaRepository businessIdeaRepository;

    @Test
    void testSaveAndRetrieveBusinessIdea() {
        // Given: A new business idea
        BusinessIdea idea = new BusinessIdea();
        idea.setTitle("Test SaaS Platform");
        idea.setRawIdea("Build a project management tool for remote teams");
        idea.setIndustry("SaaS");
        idea.setTargetMarket("Remote Teams");
        idea.setStatus(Status.SUBMITTED);
        idea.setSubmittedAt(Instant.now());
        idea.setSubmittedBy("test-user");

        // When: Save the idea
        BusinessIdea saved = businessIdeaRepository.save(idea);

        // Then: Should have an ID
        assertThat(saved.getId()).isNotNull();

        // When: Retrieve by ID
        BusinessIdea retrieved = businessIdeaRepository.findById(saved.getId()).orElse(null);

        // Then: Should match saved data
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getTitle()).isEqualTo("Test SaaS Platform");
        assertThat(retrieved.getRawIdea()).isEqualTo("Build a project management tool for remote teams");
        assertThat(retrieved.getIndustry()).isEqualTo("SaaS");
        assertThat(retrieved.getTargetMarket()).isEqualTo("Remote Teams");
        assertThat(retrieved.getStatus()).isEqualTo(Status.SUBMITTED);
        assertThat(retrieved.getSubmittedBy()).isEqualTo("test-user");
    }

    @Test
    void testUpdateBusinessIdea() {
        // Given: A saved business idea
        BusinessIdea idea = new BusinessIdea();
        idea.setTitle("Original Title");
        idea.setRawIdea("Original idea");
        idea.setStatus(Status.SUBMITTED);
        idea.setSubmittedAt(Instant.now());
        idea.setSubmittedBy("test-user");

        BusinessIdea saved = businessIdeaRepository.save(idea);
        UUID id = saved.getId();

        // When: Update the idea
        saved.setTitle("Updated Title");
        saved.setStatus(Status.ANALYZING);
        businessIdeaRepository.save(saved);

        // Then: Retrieved idea should have updates
        BusinessIdea updated = businessIdeaRepository.findById(id).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getStatus()).isEqualTo(Status.ANALYZING);
    }

    @Test
    void testDeleteBusinessIdea() {
        // Given: A saved business idea
        BusinessIdea idea = new BusinessIdea();
        idea.setTitle("To Be Deleted");
        idea.setRawIdea("This will be deleted");
        idea.setStatus(Status.SUBMITTED);
        idea.setSubmittedAt(Instant.now());
        idea.setSubmittedBy("test-user");

        BusinessIdea saved = businessIdeaRepository.save(idea);
        UUID id = saved.getId();

        // When: Delete the idea
        businessIdeaRepository.delete(saved);

        // Then: Should not be found
        assertThat(businessIdeaRepository.findById(id)).isEmpty();
    }

    @Test
    void testFindAllBusinessIdeas() {
        // Given: Multiple business ideas
        for (int i = 1; i <= 5; i++) {
            BusinessIdea idea = new BusinessIdea();
            idea.setTitle("Idea " + i);
            idea.setRawIdea("Description " + i);
            idea.setStatus(Status.SUBMITTED);
            idea.setSubmittedAt(Instant.now());
            idea.setSubmittedBy("test-user");
            businessIdeaRepository.save(idea);
        }

        // When: Find all ideas
        long count = businessIdeaRepository.count();

        // Then: Should have at least 5 ideas
        assertThat(count).isGreaterThanOrEqualTo(5);
    }

    @Test
    void testMetadataHandling() {
        // Given: Business idea with metadata
        BusinessIdea idea = new BusinessIdea();
        idea.setTitle("Idea with Metadata");
        idea.setRawIdea("Testing metadata");
        idea.setStatus(Status.SUBMITTED);
        idea.setSubmittedAt(Instant.now());
        idea.setSubmittedBy("test-user");
        
        // Add metadata
        idea.addMetadata("targetRevenue", "1000000");
        idea.addMetadata("marketSize", "50000000");
        idea.addMetadata("region", "North America");

        // When: Save and retrieve
        BusinessIdea saved = businessIdeaRepository.save(idea);
        BusinessIdea retrieved = businessIdeaRepository.findById(saved.getId()).orElse(null);

        // Then: Metadata should be preserved
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getMetadata()).hasSize(3);
        assertThat(retrieved.getMetadataValue("targetRevenue")).isEqualTo("1000000");
        assertThat(retrieved.getMetadataValue("marketSize")).isEqualTo("50000000");
        assertThat(retrieved.getMetadataValue("region")).isEqualTo("North America");
    }

    @Test
    void testStructuredProblemStatement() {
        // Given: Business idea with structured problem statement
        BusinessIdea idea = new BusinessIdea();
        idea.setTitle("Problem-Focused Idea");
        idea.setRawIdea("Help remote teams collaborate better");
        idea.setStructuredProblemStatement("WHO: Remote teams of 5-50 people\nWHAT: Lack of async collaboration\nWHY: 30% productivity loss");
        idea.setStatus(Status.SUBMITTED);
        idea.setSubmittedAt(Instant.now());
        idea.setSubmittedBy("test-user");

        // When: Save and retrieve
        BusinessIdea saved = businessIdeaRepository.save(idea);
        BusinessIdea retrieved = businessIdeaRepository.findById(saved.getId()).orElse(null);

        // Then: Structured problem statement should be preserved
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getStructuredProblemStatement()).contains("WHO:");
        assertThat(retrieved.getStructuredProblemStatement()).contains("WHAT:");
        assertThat(retrieved.getStructuredProblemStatement()).contains("WHY:");
    }

    @Test
    void testStatusTransitions() {
        // Given: A business idea
        BusinessIdea idea = new BusinessIdea();
        idea.setTitle("Status Test");
        idea.setRawIdea("Testing status transitions");
        idea.setStatus(Status.SUBMITTED);
        idea.setSubmittedAt(Instant.now());
        idea.setSubmittedBy("test-user");

        BusinessIdea saved = businessIdeaRepository.save(idea);

        // When: Transition through statuses
        saved.setStatus(Status.ANALYZING);
        businessIdeaRepository.save(saved);

        saved.setStatus(Status.SOLUTION_SYNTHESIS_IN_PROGRESS);
        businessIdeaRepository.save(saved);

        saved.setStatus(Status.COMPLETED);
        BusinessIdea finalSaved = businessIdeaRepository.save(saved);

        // Then: Final status should be COMPLETED
        BusinessIdea retrieved = businessIdeaRepository.findById(finalSaved.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getStatus()).isEqualTo(Status.COMPLETED);
    }
}
