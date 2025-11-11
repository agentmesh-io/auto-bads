package com.therighthandapp.autobads.integration;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response from AgentMesh after project initialization
 */
@Data
@Builder
public class ProjectInitializationResponse {
    private String projectId;
    private String status; // QUEUED, INITIALIZING, PLANNING, IN_PROGRESS, COMPLETED, FAILED
    private String message;
    private String githubPullRequestUrl;
    private LocalDateTime startedAt;
    private LocalDateTime estimatedCompletionAt;
    private Map<String, Object> metadata;
}
