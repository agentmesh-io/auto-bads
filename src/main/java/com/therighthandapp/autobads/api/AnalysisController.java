package com.therighthandapp.autobads.api;

import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for accessing analysis results
 * Provides endpoints for market, product, and financial analysis
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AnalysisController {

    private final BusinessIdeaRepository repository;

    /**
     * Get comprehensive analysis for an idea
     * GET /api/v1/analysis/{ideaId}
     */
    @GetMapping("/{ideaId}")
    public ResponseEntity<Map<String, Object>> getAnalysis(@PathVariable UUID ideaId) {
        log.info("GET /api/v1/analysis/{} - Fetching comprehensive analysis", ideaId);
        
        Optional<BusinessIdea> ideaOpt = repository.findById(ideaId);
        if (ideaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        BusinessIdea idea = ideaOpt.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("ideaId", idea.getId().toString());
        response.put("status", idea.getStatus().toString());
        response.put("submittedAt", idea.getSubmittedAt());
        response.put("rawIdea", idea.getRawIdea());
        response.put("structuredProblem", idea.getStructuredProblemStatement());
        
        // Include analysis results if available
        Map<String, Object> analyses = new HashMap<>();
        
        // Market analysis (from events/cache - simplified for demo)
        analyses.put("market", createMarketAnalysisSummary(idea));
        
        // Product analysis
        analyses.put("product", createProductAnalysisSummary(idea));
        
        // Financial analysis
        analyses.put("financial", createFinancialAnalysisSummary(idea));
        
        response.put("analyses", analyses);
        response.put("overallScore", calculateOverallScore(idea));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get market analysis for an idea
     * GET /api/v1/analysis/market/{ideaId}
     */
    @GetMapping("/market/{ideaId}")
    public ResponseEntity<Map<String, Object>> getMarketAnalysis(@PathVariable UUID ideaId) {
        log.info("GET /api/v1/analysis/market/{} - Fetching market analysis", ideaId);
        
        Optional<BusinessIdea> ideaOpt = repository.findById(ideaId);
        if (ideaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        BusinessIdea idea = ideaOpt.get();
        Map<String, Object> analysis = createMarketAnalysisSummary(idea);
        
        return ResponseEntity.ok(analysis);
    }

    /**
     * Get product analysis for an idea
     * GET /api/v1/analysis/product/{ideaId}
     */
    @GetMapping("/product/{ideaId}")
    public ResponseEntity<Map<String, Object>> getProductAnalysis(@PathVariable UUID ideaId) {
        log.info("GET /api/v1/analysis/product/{} - Fetching product analysis", ideaId);
        
        Optional<BusinessIdea> ideaOpt = repository.findById(ideaId);
        if (ideaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        BusinessIdea idea = ideaOpt.get();
        Map<String, Object> analysis = createProductAnalysisSummary(idea);
        
        return ResponseEntity.ok(analysis);
    }

    /**
     * Get financial analysis for an idea
     * GET /api/v1/analysis/financial/{ideaId}
     */
    @GetMapping("/financial/{ideaId}")
    public ResponseEntity<Map<String, Object>> getFinancialAnalysis(@PathVariable UUID ideaId) {
        log.info("GET /api/v1/analysis/financial/{} - Fetching financial analysis", ideaId);
        
        Optional<BusinessIdea> ideaOpt = repository.findById(ideaId);
        if (ideaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        BusinessIdea idea = ideaOpt.get();
        Map<String, Object> analysis = createFinancialAnalysisSummary(idea);
        
        return ResponseEntity.ok(analysis);
    }

    /**
     * Get analysis status (progress tracking)
     * GET /api/v1/analysis/{ideaId}/status
     */
    @GetMapping("/{ideaId}/status")
    public ResponseEntity<Map<String, Object>> getAnalysisStatus(@PathVariable UUID ideaId) {
        log.info("GET /api/v1/analysis/{}/status - Fetching analysis status", ideaId);
        
        Optional<BusinessIdea> ideaOpt = repository.findById(ideaId);
        if (ideaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        BusinessIdea idea = ideaOpt.get();
        
        Map<String, Object> status = new HashMap<>();
        status.put("ideaId", idea.getId().toString());
        status.put("overallStatus", idea.getStatus().toString());
        
        // Analysis phase progress
        Map<String, Object> phases = new HashMap<>();
        phases.put("ingestion", Map.of(
            "status", "COMPLETED",
            "completedAt", idea.getSubmittedAt()
        ));
        phases.put("market", Map.of(
            "status", "COMPLETED",
            "progress", 100
        ));
        phases.put("product", Map.of(
            "status", "COMPLETED",
            "progress", 100
        ));
        phases.put("financial", Map.of(
            "status", "COMPLETED",
            "progress", 100
        ));
        phases.put("solution", Map.of(
            "status", "IN_PROGRESS",
            "progress", 75
        ));
        
        status.put("phases", phases);
        status.put("overallProgress", 85);
        
        return ResponseEntity.ok(status);
    }

    // Helper methods to create analysis summaries
    // In production, these would fetch from analysis result tables/cache
    
    private Map<String, Object> createMarketAnalysisSummary(BusinessIdea idea) {
        Map<String, Object> market = new HashMap<>();
        market.put("marketSize", Map.of(
            "tam", "$2.5B",
            "sam", "$750M",
            "som", "$45M",
            "growthRate", "12.5%"
        ));
        market.put("competition", Map.of(
            "competitiveIntensity", "MODERATE",
            "keyCompetitors", Arrays.asList("Competitor A", "Competitor B", "Competitor C"),
            "marketConcentration", "Fragmented"
        ));
        market.put("swot", Map.of(
            "strengths", Arrays.asList("Innovative approach", "Strong team", "Low entry cost"),
            "weaknesses", Arrays.asList("Limited brand recognition", "Small market presence"),
            "opportunities", Arrays.asList("Growing market", "Digital transformation trend"),
            "threats", Arrays.asList("Market saturation", "Regulatory changes")
        ));
        market.put("pmfScore", 0.72);
        market.put("viability", "HIGH");
        market.put("recommendation", "Strong market opportunity with manageable competition");
        
        return market;
    }
    
    private Map<String, Object> createProductAnalysisSummary(BusinessIdea idea) {
        Map<String, Object> product = new HashMap<>();
        product.put("innovationScore", 0.85);
        product.put("feasibility", Map.of(
            "technical", "HIGH",
            "operational", "MEDIUM",
            "scalability", "HIGH"
        ));
        product.put("uniqueValueProposition", "AI-powered automation with human-in-the-loop design");
        product.put("keyFeatures", Arrays.asList(
            "Real-time processing",
            "Intelligent automation",
            "Customizable workflows",
            "Advanced analytics"
        ));
        product.put("developmentComplexity", "MEDIUM");
        product.put("timeToMarket", "6-9 months");
        product.put("recommendation", "Proceed with MVP development focusing on core features");
        
        return product;
    }
    
    private Map<String, Object> createFinancialAnalysisSummary(BusinessIdea idea) {
        Map<String, Object> financial = new HashMap<>();
        financial.put("developmentCost", Map.of(
            "build", "$125,000",
            "buy", "$45,000",
            "hybrid", "$75,000",
            "recommended", "hybrid"
        ));
        financial.put("operationalCost", Map.of(
            "monthly", "$8,500",
            "annual", "$102,000"
        ));
        financial.put("revenueProjection", Map.of(
            "year1", "$180,000",
            "year2", "$450,000",
            "year3", "$1,200,000"
        ));
        financial.put("roi", Map.of(
            "percentage", "185%",
            "paybackPeriod", "14 months",
            "npv", "$425,000",
            "irr", "42%"
        ));
        financial.put("riskLevel", "MODERATE");
        financial.put("fundingRequired", "$150,000");
        financial.put("recommendation", "Financially viable with strong ROI potential");
        
        return financial;
    }
    
    private double calculateOverallScore(BusinessIdea idea) {
        // Weighted average: Market (30%), Product (35%), Financial (35%)
        return 0.30 * 0.72 + 0.35 * 0.85 + 0.35 * 0.78; // = 0.79
    }
}
