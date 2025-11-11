package com.therighthandapp.autobads.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration to provide mock Kafka beans.
 * This allows tests to run without a real Kafka instance.
 */
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> kafkaTemplate() {
        // Return a mock KafkaTemplate for testing
        return mock(KafkaTemplate.class);
    }
}
