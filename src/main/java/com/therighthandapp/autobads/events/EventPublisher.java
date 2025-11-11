package com.therighthandapp.autobads.events;

import com.therighthandapp.autobads.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Event publisher service for Auto-BADS events.
 * Publishes events to Kafka topics for consumption by AgentMesh and other systems.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish IdeaValidatedEvent when a business idea passes validation
     */
    public void publishIdeaValidated(IdeaValidatedEvent event) {
        publishEvent(KafkaTopicConfig.IDEA_VALIDATED_TOPIC, event.ideaId().toString(), event);
    }

    /**
     * Publish AnalysisCompletedEvent when all analyses are finished
     */
    public void publishAnalysisCompleted(AnalysisCompletedEvent event) {
        publishEvent(KafkaTopicConfig.ANALYSIS_COMPLETED_TOPIC, event.ideaId().toString(), event);
    }

    /**
     * Publish SrsGeneratedEvent when SRS is ready for handoff to AgentMesh
     */
    public void publishSrsGenerated(SrsGeneratedEvent event) {
        publishEvent(KafkaTopicConfig.SRS_GENERATED_TOPIC, event.ideaId().toString(), event);
        log.info("Published SRS Generated Event for project: {} with correlationId: {}", 
            event.projectName(), event.correlationId());
    }

    /**
     * Generic event publishing with error handling
     */
    private void publishEvent(String topic, String key, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published event to topic: {} with key: {}", topic, key);
                } else {
                    log.error("Failed to publish event to topic: {} with key: {}. Error: {}", 
                        topic, key, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception while publishing event to topic: {} with key: {}. Error: {}", 
                topic, key, e.getMessage(), e);
        }
    }
}
