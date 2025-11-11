package com.therighthandapp.autobads.api;

import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * REST API for Software Requirements Specification (SRS) generation
 * Generates comprehensive SRS documents from analyzed business ideas
 */
@RestController
@RequestMapping("/api/v1/srs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class SrsController {

    private final BusinessIdeaRepository repository;
    
    // In-memory SRS storage (demo - should be database in production)
    private final Map<String, Map<String, Object>> srsDocuments = new HashMap<>();

    /**
     * Generate SRS document from analyzed idea
     * POST /api/v1/srs/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateSrs(@RequestBody Map<String, String> request) {
        String ideaIdStr = request.get("ideaId");
        
        if (ideaIdStr == null || ideaIdStr.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ideaId is required"));
        }
        
        UUID ideaId;
        try {
            ideaId = UUID.fromString(ideaIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid ideaId format"));
        }
        
        log.info("POST /api/v1/srs/generate - Generating SRS for idea: {}", ideaId);
        
        Optional<BusinessIdea> ideaOpt = repository.findById(ideaId);
        if (ideaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        BusinessIdea idea = ideaOpt.get();
        
        // Generate SRS document
        String srsId = UUID.randomUUID().toString();
        Map<String, Object> srs = createSrsDocument(idea, srsId);
        
        srsDocuments.put(srsId, srs);
        
        Map<String, Object> response = new HashMap<>();
        response.put("srsId", srsId);
        response.put("ideaId", ideaId.toString());
        response.put("status", "GENERATED");
        response.put("generatedAt", LocalDateTime.now());
        response.put("documentUrl", "/api/v1/srs/" + srsId);
        response.put("downloadUrl", "/api/v1/srs/" + srsId + "/download");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get SRS document by ID
     * GET /api/v1/srs/{srsId}
     */
    @GetMapping("/{srsId}")
    public ResponseEntity<Map<String, Object>> getSrs(@PathVariable String srsId) {
        log.info("GET /api/v1/srs/{} - Fetching SRS document", srsId);
        
        Map<String, Object> srs = srsDocuments.get(srsId);
        if (srs == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(srs);
    }

    /**
     * Download SRS document as text/markdown
     * GET /api/v1/srs/{srsId}/download
     */
    @GetMapping("/{srsId}/download")
    public ResponseEntity<String> downloadSrs(@PathVariable String srsId) {
        log.info("GET /api/v1/srs/{}/download - Downloading SRS document", srsId);
        
        Map<String, Object> srs = srsDocuments.get(srsId);
        if (srs == null) {
            return ResponseEntity.notFound().build();
        }
        
        String markdown = convertSrsToMarkdown(srs);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", 
            "SRS-" + srsId.substring(0, 8) + ".md");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(markdown);
    }

    /**
     * List all SRS documents for an idea
     * GET /api/v1/srs/by-idea/{ideaId}
     */
    @GetMapping("/by-idea/{ideaId}")
    public ResponseEntity<List<Map<String, Object>>> getSrsByIdea(@PathVariable UUID ideaId) {
        log.info("GET /api/v1/srs/by-idea/{} - Listing SRS documents", ideaId);
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : srsDocuments.entrySet()) {
            Map<String, Object> srs = entry.getValue();
            if (ideaId.toString().equals(srs.get("ideaId"))) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("srsId", entry.getKey());
                summary.put("ideaId", srs.get("ideaId"));
                summary.put("projectName", srs.get("projectName"));
                summary.put("version", srs.get("version"));
                summary.put("generatedAt", srs.get("generatedAt"));
                results.add(summary);
            }
        }
        
        return ResponseEntity.ok(results);
    }

    /**
     * Create comprehensive SRS document
     */
    private Map<String, Object> createSrsDocument(BusinessIdea idea, String srsId) {
        Map<String, Object> srs = new HashMap<>();
        
        // Metadata
        srs.put("srsId", srsId);
        srs.put("ideaId", idea.getId().toString());
        srs.put("projectName", extractProjectName(idea.getRawIdea()));
        srs.put("version", "1.0.0");
        srs.put("generatedAt", LocalDateTime.now());
        srs.put("generatedBy", "Auto-BADS AI System");
        
        // 1. Introduction
        Map<String, Object> introduction = new HashMap<>();
        introduction.put("purpose", "This SRS document describes the software requirements for " + 
            extractProjectName(idea.getRawIdea()) + ".");
        introduction.put("scope", idea.getStructuredProblemStatement());
        introduction.put("definitions", Arrays.asList(
            Map.of("term", "AI", "definition", "Artificial Intelligence"),
            Map.of("term", "API", "definition", "Application Programming Interface"),
            Map.of("term", "SaaS", "definition", "Software as a Service")
        ));
        introduction.put("overview", "The system will provide automated business analysis and solution generation.");
        srs.put("introduction", introduction);
        
        // 2. Overall Description
        Map<String, Object> description = new HashMap<>();
        description.put("productPerspective", "Standalone SaaS platform with API integrations");
        description.put("productFunctions", Arrays.asList(
            "User authentication and authorization",
            "Business idea submission and validation",
            "Automated market analysis",
            "Financial feasibility assessment",
            "Solution recommendation engine",
            "Report generation and export"
        ));
        description.put("userCharacteristics", Arrays.asList(
            "Entrepreneurs with basic business knowledge",
            "Business analysts with technical aptitude",
            "Product managers planning new features"
        ));
        description.put("constraints", Arrays.asList(
            "Must comply with data privacy regulations (GDPR, CCPA)",
            "Maximum response time: 3 seconds for analysis requests",
            "Support 10,000 concurrent users",
            "99.9% uptime SLA"
        ));
        description.put("assumptions", Arrays.asList(
            "Users have stable internet connection",
            "Third-party APIs remain available",
            "Market data sources are reliable"
        ));
        srs.put("overallDescription", description);
        
        // 3. Specific Requirements
        Map<String, Object> requirements = new HashMap<>();
        
        // 3.1 Functional Requirements
        List<Map<String, Object>> functionalReqs = new ArrayList<>();
        functionalReqs.add(Map.of(
            "id", "FR-1",
            "title", "User Registration",
            "description", "System shall allow users to register with email and password",
            "priority", "HIGH",
            "acceptance", "User can create account and receive confirmation email"
        ));
        functionalReqs.add(Map.of(
            "id", "FR-2",
            "title", "Idea Submission",
            "description", "System shall accept business idea descriptions via web form",
            "priority", "HIGH",
            "acceptance", "Idea is validated and stored in database"
        ));
        functionalReqs.add(Map.of(
            "id", "FR-3",
            "title", "Automated Analysis",
            "description", "System shall automatically analyze market, product, and financial viability",
            "priority", "HIGH",
            "acceptance", "Analysis completes within 60 seconds with comprehensive report"
        ));
        functionalReqs.add(Map.of(
            "id", "FR-4",
            "title", "Solution Generation",
            "description", "System shall generate Build/Buy/Hybrid solution recommendations",
            "priority", "MEDIUM",
            "acceptance", "At least 3 solutions provided with cost-benefit analysis"
        ));
        functionalReqs.add(Map.of(
            "id", "FR-5",
            "title", "Report Export",
            "description", "System shall allow users to export analysis as PDF or Markdown",
            "priority", "MEDIUM",
            "acceptance", "Report downloaded successfully with all sections"
        ));
        requirements.put("functional", functionalReqs);
        
        // 3.2 Non-Functional Requirements
        List<Map<String, Object>> nonfunctionalReqs = new ArrayList<>();
        nonfunctionalReqs.add(Map.of(
            "id", "NFR-1",
            "category", "Performance",
            "description", "System shall respond to analysis requests within 3 seconds",
            "metric", "Average response time < 3s under normal load"
        ));
        nonfunctionalReqs.add(Map.of(
            "id", "NFR-2",
            "category", "Scalability",
            "description", "System shall support 10,000 concurrent users",
            "metric", "Load test with 10K users shows < 5% error rate"
        ));
        nonfunctionalReqs.add(Map.of(
            "id", "NFR-3",
            "category", "Security",
            "description", "System shall encrypt all data in transit and at rest",
            "metric", "TLS 1.3 for transport, AES-256 for storage"
        ));
        nonfunctionalReqs.add(Map.of(
            "id", "NFR-4",
            "category", "Availability",
            "description", "System shall maintain 99.9% uptime",
            "metric", "< 8.76 hours downtime per year"
        ));
        nonfunctionalReqs.add(Map.of(
            "id", "NFR-5",
            "category", "Usability",
            "description", "System shall be accessible to users with disabilities (WCAG 2.1 AA)",
            "metric", "Automated accessibility audit passes"
        ));
        requirements.put("nonfunctional", nonfunctionalReqs);
        
        srs.put("requirements", requirements);
        
        // 4. System Architecture
        Map<String, Object> architecture = new HashMap<>();
        architecture.put("style", "Microservices with Event-Driven Architecture");
        architecture.put("components", Arrays.asList(
            Map.of("name", "API Gateway", "technology", "Spring Cloud Gateway"),
            Map.of("name", "Auth Service", "technology", "Spring Security + JWT"),
            Map.of("name", "Analysis Engine", "technology", "Spring Boot + OpenAI"),
            Map.of("name", "Database", "technology", "PostgreSQL"),
            Map.of("name", "Cache", "technology", "Redis"),
            Map.of("name", "Message Queue", "technology", "Kafka")
        ));
        architecture.put("deployment", "Kubernetes on AWS EKS");
        srs.put("architecture", architecture);
        
        // 5. Data Requirements
        Map<String, Object> data = new HashMap<>();
        data.put("entities", Arrays.asList(
            Map.of("name", "User", "attributes", Arrays.asList("id", "email", "name", "createdAt")),
            Map.of("name", "BusinessIdea", "attributes", Arrays.asList("id", "userId", "description", "status")),
            Map.of("name", "AnalysisResult", "attributes", Arrays.asList("id", "ideaId", "marketScore", "productScore", "financialScore"))
        ));
        data.put("retention", "User data retained for 7 years, analysis results for 2 years");
        srs.put("dataRequirements", data);
        
        // 6. External Interfaces
        Map<String, Object> interfaces = new HashMap<>();
        interfaces.put("userInterface", Map.of(
            "type", "Web Application (React/Next.js)",
            "browsers", Arrays.asList("Chrome 90+", "Firefox 88+", "Safari 14+", "Edge 90+"),
            "responsive", true
        ));
        interfaces.put("apiInterface", Map.of(
            "protocol", "REST over HTTPS",
            "format", "JSON",
            "authentication", "JWT Bearer Token"
        ));
        interfaces.put("externalApis", Arrays.asList(
            "OpenAI GPT-4 API",
            "Market data providers",
            "Payment gateway (Stripe)"
        ));
        srs.put("externalInterfaces", interfaces);
        
        return srs;
    }

    /**
     * Convert SRS document to Markdown format
     */
    private String convertSrsToMarkdown(Map<String, Object> srs) {
        StringBuilder md = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Header
        md.append("# Software Requirements Specification\n\n");
        md.append("**Project:** ").append(srs.get("projectName")).append("\n\n");
        md.append("**Version:** ").append(srs.get("version")).append("\n\n");
        md.append("**Generated:** ").append(
            ((LocalDateTime) srs.get("generatedAt")).format(formatter)
        ).append("\n\n");
        md.append("**Generated By:** ").append(srs.get("generatedBy")).append("\n\n");
        md.append("---\n\n");
        
        // 1. Introduction
        md.append("## 1. Introduction\n\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> intro = (Map<String, Object>) srs.get("introduction");
        md.append("### 1.1 Purpose\n\n");
        md.append(intro.get("purpose")).append("\n\n");
        md.append("### 1.2 Scope\n\n");
        md.append(intro.get("scope")).append("\n\n");
        
        // 2. Overall Description
        md.append("## 2. Overall Description\n\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> desc = (Map<String, Object>) srs.get("overallDescription");
        md.append("### 2.1 Product Functions\n\n");
        @SuppressWarnings("unchecked")
        List<String> functions = (List<String>) desc.get("productFunctions");
        for (String func : functions) {
            md.append("- ").append(func).append("\n");
        }
        md.append("\n");
        
        // 3. Requirements
        md.append("## 3. Specific Requirements\n\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> reqs = (Map<String, Object>) srs.get("requirements");
        
        md.append("### 3.1 Functional Requirements\n\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> funcReqs = (List<Map<String, Object>>) reqs.get("functional");
        for (Map<String, Object> req : funcReqs) {
            md.append("#### ").append(req.get("id")).append(": ").append(req.get("title")).append("\n\n");
            md.append("- **Description:** ").append(req.get("description")).append("\n");
            md.append("- **Priority:** ").append(req.get("priority")).append("\n");
            md.append("- **Acceptance:** ").append(req.get("acceptance")).append("\n\n");
        }
        
        md.append("### 3.2 Non-Functional Requirements\n\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nfReqs = (List<Map<String, Object>>) reqs.get("nonfunctional");
        for (Map<String, Object> req : nfReqs) {
            md.append("#### ").append(req.get("id")).append(": ").append(req.get("category")).append("\n\n");
            md.append("- **Description:** ").append(req.get("description")).append("\n");
            md.append("- **Metric:** ").append(req.get("metric")).append("\n\n");
        }
        
        // 4. Architecture
        md.append("## 4. System Architecture\n\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> arch = (Map<String, Object>) srs.get("architecture");
        md.append("**Architecture Style:** ").append(arch.get("style")).append("\n\n");
        md.append("**Key Components:**\n\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) arch.get("components");
        for (Map<String, Object> comp : components) {
            md.append("- **").append(comp.get("name")).append(":** ").append(comp.get("technology")).append("\n");
        }
        md.append("\n");
        
        md.append("---\n\n");
        md.append("*This document was automatically generated by Auto-BADS.*\n");
        
        return md.toString();
    }

    /**
     * Extract project name from raw idea
     */
    private String extractProjectName(String rawIdea) {
        // Simple extraction - in production use LLM
        if (rawIdea.toLowerCase().contains("platform")) {
            return "Business Platform";
        } else if (rawIdea.toLowerCase().contains("app")) {
            return "Mobile Application";
        } else if (rawIdea.toLowerCase().contains("system")) {
            return "Management System";
        }
        return "Software Project";
    }
}
