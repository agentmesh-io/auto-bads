package com.therighthandapp.autobads.core.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Solution Package - represents Build, Buy, or Hybrid solution alternatives
 */
@Data
@Builder
public class SolutionPackage {

    private String packageId;
    private String ideaId;
    private SolutionType type;
    private String description;
    private ArchitecturalBlueprint architecture;
    private List<Feature> features;
    private TechnicalSpecification technicalSpec;
    private ResourceRequirements resources;
    private Timeline timeline;
    private SolutionScore score;

    public enum SolutionType {
        BUILD,
        BUY,
        HYBRID
    }

    @Data
    @Builder
    public static class ArchitecturalBlueprint {
        private String systemArchitecture;
        private List<String> components;
        private List<String> integrationPoints;
        private String dataFlowDiagram;
        private List<String> technologyStack;
    }

    @Data
    @Builder
    public static class Feature {
        private String id;
        private String name;
        private String description;
        private String priority; // MUST_HAVE, SHOULD_HAVE, COULD_HAVE, WONT_HAVE
        private String acceptance_criteria;
        private int estimatedEffort; // story points
    }

    @Data
    @Builder
    public static class TechnicalSpecification {
        private String srsDocument;
        private String apiSpecification;
        private List<String> technicalConstraints;
        private List<String> qualityAttributes;
        private String securityRequirements;
    }

    @Data
    @Builder
    public static class ResourceRequirements {
        private int requiredDevelopers;
        private List<String> requiredSkills;
        private String infrastructureNeeds;
        private String vendorDependencies;
        private double capacityScore; // Internal capacity 0-100
    }

    @Data
    @Builder
    public static class Timeline {
        private int estimatedWeeks;
        private List<Milestone> milestones;
    }

    @Data
    @Builder
    public static class Milestone {
        private String name;
        private int weekNumber;
        private String deliverable;
    }

    @Data
    @Builder
    public static class SolutionScore {
        private double strategicAlignment; // 0-100
        private double technicalFeasibility; // 0-100
        private double marketOpportunity; // 0-100
        private double resourceCost; // 0-100, lower is better
        private double weightedTotalScore; // Final weighted score
        private String recommendation;
    }
}

