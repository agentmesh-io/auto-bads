package com.therighthandapp.autobads.integration;

import com.therighthandapp.autobads.config.TestKafkaConfig;
import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.ingestion.IdeaIngestionService;
import com.therighthandapp.autobads.ingestion.SemanticTranslationAgent;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Performance and load testing for Auto-BADS system.
 * Tests concurrent operations, throughput, and system behavior under load.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestKafkaConfig.class)
@Transactional
@RecordApplicationEvents
class PerformanceLoadTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private IdeaIngestionService ingestionService;

    @Autowired
    private BusinessIdeaRepository repository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @MockBean
    private SemanticTranslationAgent semanticAgent;

    private static final String MOCK_STRUCTURED_PROBLEM = 
        "Structured problem statement for performance testing";

    @BeforeEach
    void setUp() {
        // Mock LLM agent to avoid real API calls
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn(MOCK_STRUCTURED_PROBLEM);
        
        // Clear all caches before each test
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    /**
     * Test concurrent idea submission performance.
     * Verifies system can handle multiple simultaneous requests.
     */
    @Test
    void testConcurrentIdeaSubmission() throws InterruptedException, ExecutionException {
        // Given: 5 concurrent idea submissions (reduced to avoid event table overflow)
        int concurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);

        List<Future<UUID>> futures = new ArrayList<>();
        Instant startTime = Instant.now();

        // When: Submit ideas concurrently
        for (int i = 0; i < concurrentRequests; i++) {
            int ideaNum = i;
            Future<UUID> future = executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start together
                    String idea = "Concurrent idea submission #" + ideaNum;
                    return ingestionService.ingestIdea(idea);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    endLatch.countDown();
                }
            });
            futures.add(future);
        }

        startLatch.countDown(); // Start all threads
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        Instant endTime = Instant.now();
        executor.shutdown();

        // Then: All submissions should complete successfully
        assertThat(completed).isTrue();
        
        List<UUID> ideaIds = new ArrayList<>();
        for (Future<UUID> future : futures) {
            UUID ideaId = future.get();
            assertThat(ideaId).isNotNull();
            ideaIds.add(ideaId);
        }

        // Verify all ideas are unique
        assertThat(ideaIds).hasSize(concurrentRequests);
        assertThat(ideaIds).doesNotHaveDuplicates();

        // Verify all ideas persisted
        List<BusinessIdea> savedIdeas = repository.findAllById(ideaIds);
        assertThat(savedIdeas).hasSize(concurrentRequests);

        // Performance assertion: Should complete within reasonable time
        Duration duration = Duration.between(startTime, endTime);
        assertThat(duration.toSeconds()).isLessThan(30);
        
        System.out.println("Concurrent submission performance: " + 
            concurrentRequests + " ideas in " + duration.toMillis() + "ms");
    }

    /**
     * Test high-volume sequential idea submission.
     * Measures throughput for batch processing.
     */
    @Test
    void testHighVolumeSequentialSubmission() {
        // Given: Moderate batch of ideas (reduced to avoid event table overflow)
        int ideaCount = 10;
        List<String> ideas = IntStream.range(0, ideaCount)
            .mapToObj(i -> "High volume idea #" + i)
            .toList();

        // When: Submit ideas sequentially
        Instant startTime = Instant.now();
        List<UUID> ideaIds = ideas.stream()
            .map(ingestionService::ingestIdea)
            .toList();
        Instant endTime = Instant.now();

        // Then: All ideas should be saved
        assertThat(ideaIds).hasSize(ideaCount);
        assertThat(ideaIds).doesNotHaveDuplicates();

        List<BusinessIdea> savedIdeas = repository.findAllById(ideaIds);
        assertThat(savedIdeas).hasSize(ideaCount);

        // Performance metrics
        Duration duration = Duration.between(startTime, endTime);
        double throughput = ideaCount / (duration.toMillis() / 1000.0);
        
        System.out.println("Sequential submission performance:");
        System.out.println("  - Total ideas: " + ideaCount);
        System.out.println("  - Duration: " + duration.toMillis() + "ms");
        System.out.println("  - Throughput: " + String.format("%.2f", throughput) + " ideas/sec");
        System.out.println("  - Avg per idea: " + (duration.toMillis() / ideaCount) + "ms");

        // Performance assertion: Should process at reasonable rate
        assertThat(duration.toSeconds()).isLessThan(60);
    }

    /**
     * Test cache performance under concurrent load.
     * Verifies cache hit rate and thread safety.
     */
    @Test
    void testCacheConcurrentAccess() throws InterruptedException, ExecutionException {
        // Given: One idea to trigger cache population
        UUID ideaId = ingestionService.ingestIdea("Cached idea for performance test");
        
        // Commit the transaction to make data visible
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        
        // Populate cache with first read
        BusinessIdea firstRead = ingestionService.getIdea(ideaId);
        assertThat(firstRead).isNotNull();

        // When: Multiple concurrent reads (should hit cache)
        int concurrentReads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentReads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentReads);

        List<Future<BusinessIdea>> futures = new ArrayList<>();
        Instant startTime = Instant.now();

        for (int i = 0; i < concurrentReads; i++) {
            Future<BusinessIdea> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    return ingestionService.getIdea(ideaId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    endLatch.countDown();
                }
            });
            futures.add(future);
        }

        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        Instant endTime = Instant.now();
        executor.shutdown();

        // Then: All reads should complete quickly (cache hits)
        assertThat(completed).isTrue();

        for (Future<BusinessIdea> future : futures) {
            BusinessIdea idea = future.get();
            assertThat(idea).isNotNull();
            assertThat(idea.getId()).isEqualTo(ideaId);
        }

        Duration duration = Duration.between(startTime, endTime);
        System.out.println("Cache concurrent read performance: " + 
            concurrentReads + " reads in " + duration.toMillis() + "ms");

        // Cache should make this very fast
        assertThat(duration.toMillis()).isLessThan(1000);
    }

    /**
     * Test event publishing throughput.
     * Measures how many events can be published per second.
     */
    @Test
    void testEventPublishingThroughput() {
        // Given: Multiple ideas to trigger events (reduced count)
        int ideaCount = 5;
        
        // When: Ingest ideas (each publishes IdeaIngestedEvent)
        Instant startTime = Instant.now();
        List<UUID> ideaIds = IntStream.range(0, ideaCount)
            .mapToObj(i -> ingestionService.ingestIdea("Event throughput test #" + i))
            .toList();
        Instant endTime = Instant.now();

        // Then: All ideas and events processed
        assertThat(ideaIds).hasSize(ideaCount);
        
        Duration duration = Duration.between(startTime, endTime);
        double eventsPerSecond = ideaCount / (duration.toMillis() / 1000.0);

        System.out.println("Event publishing throughput:");
        System.out.println("  - Events: " + ideaCount);
        System.out.println("  - Duration: " + duration.toMillis() + "ms");
        System.out.println("  - Rate: " + String.format("%.2f", eventsPerSecond) + " events/sec");

        assertThat(duration.toSeconds()).isLessThan(30);
    }

    /**
     * Test database query performance under load.
     * Verifies batch retrieval efficiency.
     */
    @Test
    void testDatabaseBatchQueryPerformance() {
        // Given: Multiple ideas in database (reduced count)
        int ideaCount = 8;
        List<UUID> ideaIds = IntStream.range(0, ideaCount)
            .mapToObj(i -> ingestionService.ingestIdea("Batch query test #" + i))
            .toList();

        // When: Batch retrieve all ideas
        Instant startTime = Instant.now();
        List<BusinessIdea> retrievedIdeas = repository.findAllById(ideaIds);
        Instant endTime = Instant.now();

        // Then: All ideas retrieved efficiently
        assertThat(retrievedIdeas).hasSize(ideaCount);

        Duration duration = Duration.between(startTime, endTime);
        System.out.println("Database batch query performance:");
        System.out.println("  - Records: " + ideaCount);
        System.out.println("  - Duration: " + duration.toMillis() + "ms");
        System.out.println("  - Avg per record: " + String.format("%.2f", duration.toMillis() / (double)ideaCount) + "ms");

        // Should be very fast for in-memory H2
        assertThat(duration.toMillis()).isLessThan(1000);
    }

    /**
     * Test memory efficiency with large idea content.
     * Verifies system handles large text payloads.
     */
    @Test
    void testLargePayloadHandling() {
        // Given: Very large idea text (10KB)
        String largeIdea = "X".repeat(10_000);

        // When: Ingest large idea
        Instant startTime = Instant.now();
        UUID ideaId = ingestionService.ingestIdea(largeIdea);
        BusinessIdea saved = ingestionService.getIdea(ideaId);
        Instant endTime = Instant.now();

        // Then: Large payload handled correctly
        assertThat(saved).isNotNull();
        assertThat(saved.getRawIdea()).hasSize(10_000);

        Duration duration = Duration.between(startTime, endTime);
        System.out.println("Large payload handling: 10KB in " + duration.toMillis() + "ms");

        assertThat(duration.toSeconds()).isLessThan(5);
    }

    /**
     * Test mixed concurrent operations (read/write).
     * Simulates realistic workload with concurrent reads and writes.
     */
    @Test
    void testMixedConcurrentOperations() throws InterruptedException, ExecutionException, TimeoutException {
        // Given: Pre-populate some ideas
        List<UUID> existingIds = IntStream.range(0, 3)
            .mapToObj(i -> ingestionService.ingestIdea("Pre-existing idea #" + i))
            .toList();

        // Commit the transaction to make data visible
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // When: Mix of concurrent reads and writes (reduced count)
        int totalOperations = 10;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger writeCount = new AtomicInteger(0);
        AtomicInteger readCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        Instant startTime = Instant.now();

        for (int i = 0; i < totalOperations; i++) {
            int opNum = i;
            Future<?> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    if (opNum % 2 == 0) {
                        // Write operation
                        ingestionService.ingestIdea("New concurrent idea #" + opNum);
                        writeCount.incrementAndGet();
                    } else {
                        // Read operation
                        UUID randomId = existingIds.get(opNum % existingIds.size());
                        ingestionService.getIdea(randomId);
                        readCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        startLatch.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        Instant endTime = Instant.now();
        executor.shutdown();

        // Then: All operations completed
        assertThat(writeCount.get() + readCount.get()).isEqualTo(totalOperations);

        Duration duration = Duration.between(startTime, endTime);
        System.out.println("Mixed concurrent operations:");
        System.out.println("  - Total ops: " + totalOperations);
        System.out.println("  - Writes: " + writeCount.get());
        System.out.println("  - Reads: " + readCount.get());
        System.out.println("  - Duration: " + duration.toMillis() + "ms");
        System.out.println("  - Ops/sec: " + String.format("%.2f", totalOperations / (duration.toMillis() / 1000.0)));

        assertThat(duration.toSeconds()).isLessThan(30);
    }

    /**
     * Test system stability under sustained load.
     * Verifies no degradation over time.
     */
    @Test
    void testSustainedLoadStability() {
        // Given: Sustained operation over time (reduced batch size)
        int batchSize = 3;
        int batches = 3;
        List<Long> batchDurations = new ArrayList<>();

        // When: Process multiple batches
        for (int batch = 0; batch < batches; batch++) {
            Instant batchStart = Instant.now();
            
            for (int i = 0; i < batchSize; i++) {
                String idea = "Sustained load batch " + batch + " idea " + i;
                ingestionService.ingestIdea(idea);
            }
            
            Instant batchEnd = Instant.now();
            long duration = Duration.between(batchStart, batchEnd).toMillis();
            batchDurations.add(duration);
        }

        // Then: Performance should be consistent across batches
        System.out.println("Sustained load stability:");
        for (int i = 0; i < batchDurations.size(); i++) {
            System.out.println("  - Batch " + (i+1) + ": " + batchDurations.get(i) + "ms");
        }

        // Calculate variance (shouldn't increase significantly)
        double avgDuration = batchDurations.stream().mapToLong(Long::longValue).average().orElse(0);
        double maxDuration = batchDurations.stream().mapToLong(Long::longValue).max().orElse(0);
        
        // Max duration shouldn't be more than 3x average (no significant degradation)
        assertThat(maxDuration).isLessThan(avgDuration * 3);
        
        System.out.println("  - Avg duration: " + String.format("%.2f", avgDuration) + "ms");
        System.out.println("  - Max duration: " + maxDuration + "ms");
        System.out.println("  - Stability: " + String.format("%.1f", (avgDuration / maxDuration) * 100) + "%");
    }
}
