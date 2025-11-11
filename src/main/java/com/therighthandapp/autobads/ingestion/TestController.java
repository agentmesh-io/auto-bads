package com.therighthandapp.autobads.ingestion;

import com.therighthandapp.autobads.events.EventPublisher;
import com.therighthandapp.autobads.events.SrsGeneratedEvent;
import com.therighthandapp.autobads.integration.dto.SrsHandoffDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test controller for Kafka integration testing without LLM dependency
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    private final EventPublisher eventPublisher;

    public TestController(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/publish-srs")
    public ResponseEntity<Map<String, Object>> publishTestSrs(@RequestBody Map<String, String> request) {
        String projectName = request.getOrDefault("projectName", "Test Project");
        
        log.info("Publishing test SRS event to Kafka for project: {}", projectName);

        // Create a mock SRS handoff DTO
        SrsHandoffDto.SoftwareRequirementsSpecification srs = SrsHandoffDto.SoftwareRequirementsSpecification.builder()
            .version("1.0")
            .functionalRequirements(List.of(
                SrsHandoffDto.FunctionalRequirement.builder()
                    .id("FR-001")
                    .requirement("User authentication")
                    .rationale("Secure access control")
                    .testCases(List.of("Test login", "Test logout"))
                    .build()
            ))
            .nonFunctionalRequirements(List.of(
                SrsHandoffDto.NonFunctionalRequirement.builder()
                    .category("PERFORMANCE")
                    .requirement("Response time < 200ms")
                    .metric("Average response time")
                    .targetValue("200ms")
                    .build()
            ))
            .architecture(SrsHandoffDto.SystemArchitecture.builder()
                .architectureStyle("MICROSERVICES")
                .components(List.of("API Gateway", "Auth Service", "Data Service"))
                .integrationPoints(List.of("REST API", "Message Queue"))
                .databaseStrategy("PostgreSQL + Redis")
                .build())
            .dependencies(List.of("Spring Boot 3.3", "PostgreSQL 16", "Redis 7"))
            .build();

        SrsHandoffDto.FinancialProjections financials = SrsHandoffDto.FinancialProjections.builder()
            .totalCostOfOwnership(150000.0)
            .developmentCost(80000.0)
            .operationalCostPerYear(15000.0)
            .expectedRoi(2.5)
            .breakEvenMonths(18)
            .build();

        SrsHandoffDto.RiskAssessment risks = SrsHandoffDto.RiskAssessment.builder()
            .overallRiskLevel("MEDIUM")
            .identifiedRisks(List.of(
                SrsHandoffDto.Risk.builder()
                    .category("TECHNICAL")
                    .description("Integration complexity")
                    .severity("MEDIUM")
                    .likelihood("MEDIUM")
                    .mitigationStrategies(List.of("Incremental integration", "API versioning"))
                    .build()
            ))
            .build();

        SrsHandoffDto srsHandoff = SrsHandoffDto.builder()
            .ideaId(UUID.randomUUID())
            .ideaTitle(projectName)
            .generatedAt(LocalDateTime.now())
            .businessCase("Test business case for Kafka integration")
            .problemStatement("Testing event-driven architecture")
            .strategicAlignment("High")
            .srs(srs)
            .prioritizedBacklog(List.of(
                SrsHandoffDto.Feature.builder()
                    .id("F-001")
                    .name("User Management")
                    .description("Complete user lifecycle management")
                    .priority("HIGH")
                    .acceptanceCriteria(List.of("Users can register", "Users can login"))
                    .estimatedEffort(13)
                    .build()
            ))
            .technicalConstraints(List.of("Must use Java 21", "Must be cloud-native"))
            .qualityAttributes(List.of("Scalability", "Security", "Maintainability"))
            .financials(financials)
            .riskAssessment(risks)
            .recommendedSolutionType("BUILD")
            .recommendationScore(85.0)
            .metadata(Map.of("test", true, "environment", "development"))
            .build();

        // Create and publish event
        SrsGeneratedEvent event = SrsGeneratedEvent.of(
            System.currentTimeMillis(),
            projectName,
            srsHandoff
        );

        eventPublisher.publishSrsGenerated(event);
        
        log.info("Test SRS event published successfully. CorrelationId: {}", event.correlationId());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "SRS event published to Kafka topic 'autobads.srs.generated'",
            "projectName", projectName,
            "correlationId", event.correlationId(),
            "ideaId", event.ideaId()
        ));
    }
}
