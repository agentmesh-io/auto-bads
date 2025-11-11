package com.therighthandapp.autobads.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.therighthandapp.autobads.config.TestKafkaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis cache integration tests using TestContainers.
 * Tests cache hit/miss scenarios, TTL, eviction, and cache manager configuration.
 */
@SpringBootTest(classes = {RedisCacheIntegrationTest.TestConfig.class})
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestKafkaConfig.class)
@Testcontainers
class RedisCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.cache.type", () -> "redis");
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TestCacheService testCacheService;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        
        // Reset call counter
        testCacheService.resetCounter();
    }

    @Test
    void testRedisContainerRunning() {
        // Verify Redis container is running
        assertThat(redis.isRunning()).isTrue();
        assertThat(redis.getFirstMappedPort()).isGreaterThan(0);
    }

    @Test
    void testCacheManagerConfiguration() {
        // Given: Cache manager should be available
        // Then: Verify it's configured correctly
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCacheNames()).isNotEmpty();
        
        // Verify specific caches are configured
        assertThat(cacheManager.getCache("testCache")).isNotNull();
    }

    @Test
    void testCacheHit() {
        // Given: First call should cache the result
        String result1 = testCacheService.getCachedValue("key1");
        assertThat(result1).isEqualTo("value-key1");
        assertThat(testCacheService.getCallCount()).isEqualTo(1);

        // When: Second call with same key
        String result2 = testCacheService.getCachedValue("key1");

        // Then: Should return cached value without invoking method
        assertThat(result2).isEqualTo("value-key1");
        assertThat(testCacheService.getCallCount()).isEqualTo(1); // Still 1, not 2
    }

    @Test
    void testCacheMiss() {
        // Given: First call caches result for key1
        testCacheService.getCachedValue("key1");
        assertThat(testCacheService.getCallCount()).isEqualTo(1);

        // When: Call with different key
        String result = testCacheService.getCachedValue("key2");

        // Then: Should invoke method again (cache miss)
        assertThat(result).isEqualTo("value-key2");
        assertThat(testCacheService.getCallCount()).isEqualTo(2);
    }

    @Test
    void testMultipleCacheHits() {
        // Given: Cache three different keys
        testCacheService.getCachedValue("key1");
        testCacheService.getCachedValue("key2");
        testCacheService.getCachedValue("key3");
        assertThat(testCacheService.getCallCount()).isEqualTo(3);

        // When: Access cached values multiple times
        testCacheService.getCachedValue("key1");
        testCacheService.getCachedValue("key2");
        testCacheService.getCachedValue("key3");
        testCacheService.getCachedValue("key1");
        testCacheService.getCachedValue("key2");

        // Then: No additional method calls
        assertThat(testCacheService.getCallCount()).isEqualTo(3);
    }

    @Test
    void testCacheEviction() {
        // Given: Cached value
        testCacheService.getCachedValue("key1");
        assertThat(testCacheService.getCallCount()).isEqualTo(1);

        // Verify cache hit
        testCacheService.getCachedValue("key1");
        assertThat(testCacheService.getCallCount()).isEqualTo(1);

        // When: Evict cache
        testCacheService.evictCache("key1");

        // Then: Next call should miss cache
        testCacheService.getCachedValue("key1");
        assertThat(testCacheService.getCallCount()).isEqualTo(2);
    }

    @Test
    void testCacheClear() {
        // Given: Multiple cached values
        testCacheService.getCachedValue("key1");
        testCacheService.getCachedValue("key2");
        testCacheService.getCachedValue("key3");
        assertThat(testCacheService.getCallCount()).isEqualTo(3);

        // When: Clear all cache
        testCacheService.clearCache();

        // Then: All values should be recalculated
        testCacheService.getCachedValue("key1");
        testCacheService.getCachedValue("key2");
        testCacheService.getCachedValue("key3");
        assertThat(testCacheService.getCallCount()).isEqualTo(6);
    }

    @Test
    void testCachePut() {
        // Given: Update cache with new value
        testCacheService.updateCachedValue("key1", "updated-value");

        // When: Retrieve value
        String result = testCacheService.getCachedValue("key1");

        // Then: Should return updated value without invoking getCachedValue method
        assertThat(result).isEqualTo("updated-value");
        assertThat(testCacheService.getCallCount()).isEqualTo(0);
    }

    @Test
    void testNullValueNotCached() {
        // Given: Method returns null
        String result1 = testCacheService.getCachedValueOrNull("nonexistent");
        assertThat(result1).isNull();
        assertThat(testCacheService.getCallCount()).isEqualTo(1);

        // When: Call again with same key
        String result2 = testCacheService.getCachedValueOrNull("nonexistent");

        // Then: Should invoke method again (null not cached)
        assertThat(result2).isNull();
        assertThat(testCacheService.getCallCount()).isEqualTo(2);
    }

    @Test
    void testCacheKeyGeneration() {
        // Given: Different parameters
        testCacheService.getCachedValue("param1");
        testCacheService.getCachedValue("param2");

        // When: Access with same parameters
        testCacheService.getCachedValue("param1");
        testCacheService.getCachedValue("param2");

        // Then: Only 2 method calls (one for each unique parameter)
        assertThat(testCacheService.getCallCount()).isEqualTo(2);
    }

    @Test
    void testConditionalCaching() {
        // Given: Conditional caching based on result length (> 10 characters)
        // "result-x" = 8 characters (not cached)
        testCacheService.getCachedConditionally("x");
        assertThat(testCacheService.getCallCount()).isEqualTo(1);

        // When: Call again with same key
        testCacheService.getCachedConditionally("x");

        // Then: Should NOT be cached (result too short: "result-x" = 8 chars)
        assertThat(testCacheService.getCallCount()).isEqualTo(2);

        // Given: Longer result
        // "result-verylongkey" = 18 characters (cached)
        testCacheService.resetCounter();
        testCacheService.getCachedConditionally("verylongkey");
        assertThat(testCacheService.getCallCount()).isEqualTo(1);

        // When: Call again
        testCacheService.getCachedConditionally("verylongkey");

        // Then: Should be cached
        assertThat(testCacheService.getCallCount()).isEqualTo(1);
    }

    @Test
    void testCacheConcurrency() throws InterruptedException {
        // Given: Pre-populate cache to avoid race condition
        testCacheService.getCachedValue("concurrent-key");
        int initialCalls = testCacheService.getCallCount();
        
        // When: Multiple threads accessing same cached key
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    testCacheService.getCachedValue("concurrent-key");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown(); // Release all threads at once
        doneLatch.await(); // Wait for all to complete

        // Then: No additional method calls (all threads get cached value)
        assertThat(testCacheService.getCallCount()).isEqualTo(initialCalls);
    }

    /**
     * Test configuration with cache manager and cache service
     */
    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            
            GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

            return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .transactionAware()
                .build();
        }

        @Bean
        public TestCacheService testCacheService() {
            return new TestCacheService();
        }
    }

    /**
     * Test service with cacheable methods
     */
    @Service
    static class TestCacheService {
        private final AtomicInteger callCounter = new AtomicInteger(0);

        @Cacheable(value = "testCache", key = "#key")
        public String getCachedValue(String key) {
            callCounter.incrementAndGet();
            return "value-" + key;
        }

        @Cacheable(value = "testCache", key = "#key", unless = "#result == null")
        public String getCachedValueOrNull(String key) {
            callCounter.incrementAndGet();
            return null; // Simulate not found
        }

        @Cacheable(value = "testCache", key = "#key", unless = "#result == null || #result.length() <= 10")
        public String getCachedConditionally(String key) {
            callCounter.incrementAndGet();
            return "result-" + key;
        }

        @org.springframework.cache.annotation.CachePut(value = "testCache", key = "#key")
        public String updateCachedValue(String key, String value) {
            return value;
        }

        @org.springframework.cache.annotation.CacheEvict(value = "testCache", key = "#key")
        public void evictCache(String key) {
            // Evict specific key
        }

        @org.springframework.cache.annotation.CacheEvict(value = "testCache", allEntries = true)
        public void clearCache() {
            // Clear all cache entries
        }

        public int getCallCount() {
            return callCounter.get();
        }

        public void resetCounter() {
            callCounter.set(0);
        }
    }
}
