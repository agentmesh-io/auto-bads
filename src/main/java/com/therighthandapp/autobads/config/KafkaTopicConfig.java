package com.therighthandapp.autobads.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration for Auto-BADS event-driven architecture.
 * Defines topics for publishing events to AgentMesh and other consumers.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String IDEA_VALIDATED_TOPIC = "autobads.idea.validated";
    public static final String ANALYSIS_COMPLETED_TOPIC = "autobads.analysis.completed";
    public static final String SRS_GENERATED_TOPIC = "autobads.srs.generated";
    
    // Dead Letter Topics for error handling
    public static final String IDEA_VALIDATED_DLT = "autobads.idea.validated.dlt";
    public static final String ANALYSIS_COMPLETED_DLT = "autobads.analysis.completed.dlt";
    public static final String SRS_GENERATED_DLT = "autobads.srs.generated.dlt";

    @Bean
    public NewTopic ideaValidatedTopic() {
        return TopicBuilder
            .name(IDEA_VALIDATED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic analysisCompletedTopic() {
        return TopicBuilder
            .name(ANALYSIS_COMPLETED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic srsGeneratedTopic() {
        return TopicBuilder
            .name(SRS_GENERATED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic ideaValidatedDltTopic() {
        return TopicBuilder
            .name(IDEA_VALIDATED_DLT)
            .partitions(1)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic analysisCompletedDltTopic() {
        return TopicBuilder
            .name(ANALYSIS_COMPLETED_DLT)
            .partitions(1)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic srsGeneratedDltTopic() {
        return TopicBuilder
            .name(SRS_GENERATED_DLT)
            .partitions(1)
            .replicas(1)
            .build();
    }
}
