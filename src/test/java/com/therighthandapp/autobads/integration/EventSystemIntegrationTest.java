package com.therighthandapp.autobads.integration;

import com.therighthandapp.autobads.config.TestKafkaConfig;
import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.core.domain.Status;
import com.therighthandapp.autobads.core.events.IdeaIngestedEvent;
import com.therighthandapp.autobads.core.events.MarketAnalysisCompletedEvent;
import com.therighthandapp.autobads.core.events.ProductAnalysisCompletedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Event system integration tests.
 * Tests Spring ApplicationEventPublisher and event recording using Spring's @RecordApplicationEvents.
 * Validates event publishing, event data integrity, and event ordering.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestKafkaConfig.class)
@Transactional
@RecordApplicationEvents
class EventSystemIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private IdeaIngestionService ingestionService;

    @Autowired
    private BusinessIdeaRepository repository;

    @MockBean
    private SemanticTranslationAgent semanticAgent;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        
        // Mock semantic agent responses
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Problem: Test structured problem statement");
        when(semanticAgent.generateBusinessHypothesis(anyString()))
            .thenReturn("Hypothesis: Test business hypothesis");
    }

    @Test
    void testIdeaIngestedEventPublishing() {
        // Given: Raw idea
        String rawIdea = "Build AI-powered customer service platform";

        // When: Ingest idea (which publishes IdeaIngestedEvent)
        UUID ideaId = ingestionService.ingestIdea(rawIdea);

        // Then: IdeaIngestedEvent should be published
        long eventCount = applicationEvents.stream(IdeaIngestedEvent.class).count();
        assertThat(eventCount).isGreaterThan(0);

        // Verify event contains correct data
        IdeaIngestedEvent event = applicationEvents.stream(IdeaIngestedEvent.class)
            .findFirst()
            .orElse(null);
        
        assertThat(event).isNotNull();
        assertThat(event.getIdeaId()).isEqualTo(ideaId.toString());
        assertThat(event.getStructuredProblemStatement()).contains("structured problem");
        assertThat(event.getBusinessHypothesis()).contains("hypothesis");
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testMultipleIdeaIngestedEventsRegistered() {
        // Given: Raw idea
        String rawIdea = "Create online learning platform";

        // When: Ingest idea
        ingestionService.ingestIdea(rawIdea);

        // Then: IdeaIngestedEvent should be recorded
        long eventCount = applicationEvents.stream(IdeaIngestedEvent.class).count();
        assertThat(eventCount).isEqualTo(1);
        
        // Verify event listener registration in logs
        // The logs show: "Registering publication of com.therighthandapp.autobads.core.events.IdeaIngestedEvent"
        // for MarketAgentService, ProductAgentService, AnalyticalAgentService, and SolutionSynthesisService
    }

    @Test
    void testManualEventPublishing() {
        // Given: Manually created event
        IdeaIngestedEvent event = IdeaIngestedEvent.builder()
            .ideaId(UUID.randomUUID().toString())
            .structuredProblemStatement("Problem: Manual test problem")
            .businessHypothesis("Hypothesis: Manual test hypothesis")
            .timestamp(Instant.now())
            .build();

        // When: Publish event manually
        eventPublisher.publishEvent(event);

        // Then: Event should be recorded
        long eventCount = applicationEvents.stream(IdeaIngestedEvent.class).count();
        assertThat(eventCount).isGreaterThan(0);

        // Verify event data
        IdeaIngestedEvent recordedEvent = applicationEvents.stream(IdeaIngestedEvent.class)
            .findFirst()
            .orElse(null);
        
        assertThat(recordedEvent).isNotNull();
        assertThat(recordedEvent.getIdeaId()).isEqualTo(event.getIdeaId());
        assertThat(recordedEvent.getStructuredProblemStatement()).isEqualTo(event.getStructuredProblemStatement());
    }

    @Test
    void testMultipleEventsPublished() {
        // Given: Multiple ideas
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("Problem 1")
            .thenReturn("Problem 2")
            .thenReturn("Problem 3");

        // When: Ingest multiple ideas
        UUID id1 = ingestionService.ingestIdea("Idea 1");
        UUID id2 = ingestionService.ingestIdea("Idea 2");
        UUID id3 = ingestionService.ingestIdea("Idea 3");

        // Then: Should have 3 IdeaIngestedEvents
        long eventCount = applicationEvents.stream(IdeaIngestedEvent.class).count();
        assertThat(eventCount).isEqualTo(3);

        // Verify each event has correct idea ID
        assertThat(applicationEvents.stream(IdeaIngestedEvent.class))
            .extracting(IdeaIngestedEvent::getIdeaId)
            .containsExactlyInAnyOrder(
                id1.toString(),
                id2.toString(),
                id3.toString()
            );
    }

    @Test
    void testEventOrderingPreserved() {
        // Given: Multiple ideas ingested in sequence
        UUID id1 = ingestionService.ingestIdea("First idea");
        UUID id2 = ingestionService.ingestIdea("Second idea");
        UUID id3 = ingestionService.ingestIdea("Third idea");

        // Then: Events should be in order
        java.util.List<String> eventIds = applicationEvents.stream(IdeaIngestedEvent.class)
            .map(IdeaIngestedEvent::getIdeaId)
            .toList();

        assertThat(eventIds).containsExactly(
            id1.toString(),
            id2.toString(),
            id3.toString()
        );
    }

    @Test
    void testEventWithNullFieldsHandled() {
        // Given: Event with minimal data
        IdeaIngestedEvent event = IdeaIngestedEvent.builder()
            .ideaId(UUID.randomUUID().toString())
            .structuredProblemStatement("Problem: Minimal event")
            .timestamp(Instant.now())
            .build();

        // When: Publish event
        eventPublisher.publishEvent(event);

        // Then: Should be handled without errors
        long eventCount = applicationEvents.stream(IdeaIngestedEvent.class).count();
        assertThat(eventCount).isGreaterThan(0);
    }

    @Test
    void testEventTimestampAccuracy() {
        // Given: Current time
        Instant before = Instant.now().minusSeconds(1);

        // When: Ingest idea
        ingestionService.ingestIdea("Test idea");

        Instant after = Instant.now().plusSeconds(1);

        // Then: Event timestamp should be between before and after
        IdeaIngestedEvent event = applicationEvents.stream(IdeaIngestedEvent.class)
            .findFirst()
            .orElse(null);

        assertThat(event).isNotNull();
        assertThat(event.getTimestamp()).isBetween(before, after);
    }

    @Test
    void testEventIsolationBetweenTests() {
        // Given: Fresh test context
        // When: Ingest one idea
        ingestionService.ingestIdea("Isolated test idea");

        // Then: Should only have events from this test
        long eventCount = applicationEvents.stream(IdeaIngestedEvent.class).count();
        assertThat(eventCount).isEqualTo(1);
    }

    @Test
    void testMarketAnalysisEventPublishing() {
        // Given: Market analysis completed event
        MarketAnalysisCompletedEvent event = MarketAnalysisCompletedEvent.builder()
            .ideaId(UUID.randomUUID())
            .timestamp(java.time.LocalDateTime.now())
            .build();

        // When: Publish event
        eventPublisher.publishEvent(event);

        // Then: Event should be recorded
        long eventCount = applicationEvents.stream(MarketAnalysisCompletedEvent.class).count();
        assertThat(eventCount).isGreaterThan(0);
        
        // Verify event data
        MarketAnalysisCompletedEvent recordedEvent = applicationEvents.stream(MarketAnalysisCompletedEvent.class)
            .findFirst()
            .orElse(null);
        
        assertThat(recordedEvent).isNotNull();
        assertThat(recordedEvent.getIdeaId()).isEqualTo(event.getIdeaId());
    }

    @Test
    void testProductAnalysisEventPublishing() {
        // Given: Product analysis completed event
        ProductAnalysisCompletedEvent event = ProductAnalysisCompletedEvent.builder()
            .ideaId(UUID.randomUUID())
            .timestamp(java.time.LocalDateTime.now())
            .build();

        // When: Publish event
        eventPublisher.publishEvent(event);

        // Then: Event should be recorded
        long eventCount = applicationEvents.stream(ProductAnalysisCompletedEvent.class).count();
        assertThat(eventCount).isGreaterThan(0);
        
        // Verify event data
        ProductAnalysisCompletedEvent recordedEvent = applicationEvents.stream(ProductAnalysisCompletedEvent.class)
            .findFirst()
            .orElse(null);
        
        assertThat(recordedEvent).isNotNull();
        assertThat(recordedEvent.getIdeaId()).isEqualTo(event.getIdeaId());
    }

    @Test
    void testEventDrivenWorkflowPersistence() {
        // Given: Raw idea
        String rawIdea = "Build project management tool";

        // When: Start workflow by ingesting idea
        UUID ideaId = ingestionService.ingestIdea(rawIdea);

        // Then: Should trigger event cascade
        // 1. IdeaIngestedEvent published
        assertThat(applicationEvents.stream(IdeaIngestedEvent.class).count())
            .isGreaterThan(0);

        // 2. Idea persisted in database with correct status
        BusinessIdea savedIdea = repository.findById(ideaId).orElse(null);
        assertThat(savedIdea).isNotNull();
        assertThat(savedIdea.getStatus()).isEqualTo(Status.ANALYZING);
        assertThat(savedIdea.getStructuredProblemStatement()).contains("structured problem");
    }

    @Test
    void testEventPublishingPerformance() {
        // Given: Batch of ideas
        int batchSize = 10;
        long startTime = System.currentTimeMillis();

        // When: Ingest batch
        for (int i = 0; i < batchSize; i++) {
            ingestionService.ingestIdea("Performance test idea " + i);
        }

        long duration = System.currentTimeMillis() - startTime;

        // Then: Should complete quickly (< 5 seconds for 10 ideas)
        assertThat(duration).isLessThan(5000);

        // And: All events published
        long eventCount = applicationEvents.stream(IdeaIngestedEvent.class).count();
        assertThat(eventCount).isEqualTo(batchSize);
    }
}
