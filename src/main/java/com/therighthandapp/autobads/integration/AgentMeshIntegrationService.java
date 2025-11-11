package com.therighthandapp.autobads.integration;

import com.therighthandapp.autobads.integration.dto.ProjectInitializationDto;
import com.therighthandapp.autobads.integration.dto.SrsHandoffDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Integration Service for communicating with AgentMesh
 * Implements circuit breaker and retry patterns for resilience
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMeshIntegrationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${agentmesh.api.url:http://localhost:8080}")
    private String agentMeshApiUrl;
    
    @Value("${agentmesh.api.key:}")
    private String apiKey;
    
    /**
     * Hand off validated SRS to AgentMesh for implementation
     * 
     * @param srsHandoff The structured SRS and requirements
     * @return ProjectInitializationResponse with project tracking info
     */
    @CircuitBreaker(name = "agentmesh", fallbackMethod = "handoffFallback")
    @Retry(name = "agentmesh")
    public ProjectInitializationResponse handoffToAgentMesh(SrsHandoffDto srsHandoff) {
        log.info("Handing off SRS for idea {} to AgentMesh", srsHandoff.getIdeaId());
        
        // Transform SRS to AgentMesh project initialization format
        ProjectInitializationDto projectInit = buildProjectInitialization(srsHandoff);
        
        // Prepare HTTP request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-API-Key", apiKey);
        }
        
        HttpEntity<ProjectInitializationDto> request = new HttpEntity<>(projectInit, headers);
        
        // POST to AgentMesh project initialization endpoint
        String url = agentMeshApiUrl + "/api/projects/initialize";
        log.debug("Sending request to: {}", url);
        
        try {
            ResponseEntity<ProjectInitializationResponse> response = restTemplate.postForEntity(
                url,
                request,
                ProjectInitializationResponse.class
            );
            
            ProjectInitializationResponse responseBody = response.getBody();
            if (responseBody != null) {
                log.info("Project initialized in AgentMesh: projectId={}, status={}", 
                    responseBody.getProjectId(), responseBody.getStatus());
                return responseBody;
            }
            
            throw new IntegrationException("Empty response from AgentMesh");
            
        } catch (Exception e) {
            log.error("Error communicating with AgentMesh", e);
            throw new IntegrationException("Failed to handoff to AgentMesh: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fallback method when circuit breaker opens
     */
    private ProjectInitializationResponse handoffFallback(SrsHandoffDto srsHandoff, Exception e) {
        log.error("Circuit breaker OPEN: Cannot reach AgentMesh. Idea: {}", 
            srsHandoff.getIdeaId(), e);
        
        return ProjectInitializationResponse.builder()
            .projectId(srsHandoff.getIdeaId().toString())
            .status("QUEUED")
            .message("AgentMesh is temporarily unavailable. Your request has been queued.")
            .build();
    }
    
    /**
     * Build ProjectInitializationDto from SRS handoff
     */
    private ProjectInitializationDto buildProjectInitialization(SrsHandoffDto srsHandoff) {
        return ProjectInitializationDto.builder()
            .projectId(srsHandoff.getIdeaId())
            .projectName(srsHandoff.getIdeaTitle())
            .projectDescription(srsHandoff.getProblemStatement())
            .requirements(srsHandoff)
            .workflow(ProjectInitializationDto.WorkflowConfiguration.builder()
                .executionStrategy("ADAPTIVE")
                .maxIterations(5)
                .enableMastMonitoring(true)
                .enableGithubIntegration(true)
                .qualityGates(ProjectInitializationDto.QualityGates.builder()
                    .minTestCoverage(80)
                    .requireCodeReview(true)
                    .requireSecurityScan(true)
                    .maxCyclomaticComplexity(10)
                    .build())
                .build())
            .priority("MEDIUM")
            .metadata(srsHandoff.getMetadata())
            .build();
    }
    
    /**
     * Check status of a project in AgentMesh
     */
    @CircuitBreaker(name = "agentmesh")
    @Retry(name = "agentmesh")
    public ProjectStatus getProjectStatus(String projectId) {
        log.info("Checking project status in AgentMesh: {}", projectId);
        
        String url = agentMeshApiUrl + "/api/projects/" + projectId + "/status";
        
        try {
            ResponseEntity<ProjectStatus> response = restTemplate.getForEntity(
                url,
                ProjectStatus.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Error getting project status from AgentMesh", e);
            throw new IntegrationException("Failed to get project status: " + e.getMessage(), e);
        }
    }
}
