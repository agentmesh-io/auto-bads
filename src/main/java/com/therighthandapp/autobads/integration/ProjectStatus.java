package com.therighthandapp.autobads.integration;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Current status of a project in AgentMesh
 */
@Data
@Builder
public class ProjectStatus {
    private String projectId;
    private String status; // QUEUED, PLANNING, CODING, TESTING, REVIEWING, COMPLETED, FAILED
    private String currentPhase;
    private Integer completionPercentage;
    private LocalDateTime lastUpdated;
    private String githubPullRequestUrl;
    private String errorMessage;
}
