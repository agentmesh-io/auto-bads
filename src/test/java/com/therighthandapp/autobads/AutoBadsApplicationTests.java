package com.therighthandapp.autobads;

import com.therighthandapp.autobads.config.TestKafkaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic application context test
 */
@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestKafkaConfig.class)
class AutoBadsApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies the Spring context loads correctly
    }
}
