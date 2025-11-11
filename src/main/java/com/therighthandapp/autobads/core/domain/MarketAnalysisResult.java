package com.therighthandapp.autobads.core.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Structured output model for Market Analysis
 * Includes SWOT, PESTEL, competitive intelligence, and PMF assessment
 */
@Data
@Builder
public class MarketAnalysisResult {
    
    private String ideaId;
    private SwotAnalysis swot;
    private PestelAnalysis pestel;
    private CompetitiveAnalysis competitive;
    private ProductMarketFit pmfAssessment;
    private List<MarketOpportunity> opportunities;
    private double marketOpportunityScore; // 0-100
    
    @Data
    @Builder
    public static class SwotAnalysis {
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> opportunities;
        private List<String> threats;
        private Map<String, String> strategicConnections;
    }
    
    @Data
    @Builder
    public static class PestelAnalysis {
        private List<String> political;
        private List<String> economic;
        private List<String> social;
        private List<String> technological;
        private List<String> environmental;
        private List<String> legal;
    }
    
    @Data
    @Builder
    public static class CompetitiveAnalysis {
        private List<Competitor> competitors;
        private String competitiveGapAnalysis;
        private double marketTrendVelocity; // 0-5, rate of market change
    }
    
    @Data
    @Builder
    public static class Competitor {
        private String name;
        private String positioning;
        private List<String> strengths;
        private List<String> weaknesses;
        private String recentMoves;
    }
    
    @Data
    @Builder
    public static class ProductMarketFit {
        private double customerRetentionRate; // 0-1
        private double churnRate; // 0-1
        private double npsScore; // -100 to 100
        private double fortyPercentRule; // PMF threshold: >40% very disappointed
        private String assessment;
    }
    
    @Data
    @Builder
    public static class MarketOpportunity {
        private String description;
        private String targetSegment;
        private double estimatedValue;
        private String urgency;
    }
}

