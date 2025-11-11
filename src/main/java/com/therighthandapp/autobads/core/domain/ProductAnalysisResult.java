package com.therighthandapp.autobads.core.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Structured output model for Product Analysis
 * Evaluates innovation, scalability, and applies innovation frameworks
 */
@Data
@Builder
public class ProductAnalysisResult {

    private String ideaId;
    private InnovationAssessment innovation;
    private ScalabilityAssessment scalability;
    private UserEngagementPotential userEngagement;
    private DesignThinkingInsights designThinking;
    private DisruptiveInnovationEvaluation disruptiveEval;
    private TrizAnalysis trizAnalysis;
    private double productScore; // 0-100

    @Data
    @Builder
    public static class InnovationAssessment {
        private double innovationLevel; // 0-100
        private String innovationType; // Incremental, Radical, Disruptive
        private List<String> uniqueValuePropositions;
        private List<String> innovationRisks;
    }

    @Data
    @Builder
    public static class ScalabilityAssessment {
        private double scalabilityScore; // 0-100
        private List<String> scalabilityFactors;
        private List<String> scalabilityConstraints;
        private String recommendedArchitecture;
    }

    @Data
    @Builder
    public static class UserEngagementPotential {
        private double engagementScore; // 0-100
        private List<String> engagementDrivers;
        private String targetUserPersona;
        private List<String> painPointsAddressed;
    }

    @Data
    @Builder
    public static class DesignThinkingInsights {
        private String empathyFindings;
        private String problemDefinition;
        private List<String> generatedIdeas;
        private String customerJourneyMap;
        private List<String> servicePainPoints;
    }

    @Data
    @Builder
    public static class DisruptiveInnovationEvaluation {
        private boolean isDisruptive;
        private String targetMarketSegment; // Low-end or new market
        private double productQualityAssessment;
        private String businessModelType;
        private double disruptivePotentialScore; // 0-100
    }

    @Data
    @Builder
    public static class TrizAnalysis {
        private List<String> technicalContradictions;
        private List<String> physicalContradictions;
        private List<Integer> applicableInventivePrinciples;
        private List<String> proposedInnovativeSolutions;
    }
}

