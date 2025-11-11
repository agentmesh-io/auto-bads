package com.therighthandapp.autobads.integration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for initializing a new project in AgentMesh
 * This is sent from Auto-BADS to AgentMesh after SRS generation is complete
 */
@Data
@Builder
public class ProjectInitializationDto {
    
    // Project Identification
    private UUID projectId;
    private String projectName;
    private String projectDescription;
    private LocalDateTime initiatedAt;
    
    // Requirements from Auto-BADS
    private SrsHandoffDto requirements;
    
    // Workflow Configuration
    private WorkflowConfiguration workflow;
    
    // Repository Information
    private String githubRepo;
    private String githubOwner;
    private String githubBranch;
    
    // Tenant Information (for multi-tenancy)
    private String tenantId;
    private String tenantTier; // FREE, PROFESSIONAL, ENTERPRISE
    
    // Priority and Timeline
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
    private LocalDateTime targetCompletionDate;
    
    // Metadata
    private Map<String, Object> metadata;
    
    /**
     * Workflow configuration for AgentMesh execution
     */
    @Data
    @Builder
    public static class WorkflowConfiguration {
        private String executionStrategy; // LINEAR, PARALLEL, ADAPTIVE
        private Integer maxIterations; // Maximum self-correction iterations
        private Boolean enableMastMonitoring; // Enable MAST failure detection
        private Boolean enableGithubIntegration; // Auto-create PRs
        private QualityGates qualityGates;
    }
    
    /**
     * Quality gates that must be passed
     */
    @Data
    @Builder
    public static class QualityGates {
        private Integer minTestCoverage; // Percentage
        private Boolean requireCodeReview;
        private Boolean requireSecurityScan;
        private Integer maxCyclomaticComplexity;
    }
}
