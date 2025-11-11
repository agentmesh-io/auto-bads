package com.therighthandapp.autobads.core.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Structured output model for Financial and Risk Analysis
 * Includes TCO, NPV, and risk assessments from hybrid LLM/DL models
 */
@Data
@Builder
public class FinancialAnalysisResult {

    private String ideaId;
    private TotalCostOfOwnership tco;
    private FinancialForecast forecast;
    private RiskAssessment riskAssessment;
    private Map<String, String> xaiExplanations; // XAI narratives
    private double financialViabilityScore; // 0-100

    @Data
    @Builder
    public static class TotalCostOfOwnership {
        private BigDecimal initialInvestment;
        private BigDecimal yearlyOperationalCost;
        private BigDecimal maintenanceCost;
        private BigDecimal thirdPartyLicensing;
        private BigDecimal internalResourceCost;
        private BigDecimal totalFiveYearTco;
        private String costBreakdown;
    }

    @Data
    @Builder
    public static class FinancialForecast {
        private BigDecimal predictedNpv;
        private double roi; // Return on Investment %
        private int breakEvenMonths;
        private List<BigDecimal> yearlyRevenueForecast; // 5 years
        private List<BigDecimal> yearlyProfitForecast; // 5 years
        private String modelType; // LSTM, GRU, Transformer
        private double confidenceLevel; // Model confidence
        private String sentimentIndex; // LLM-derived market sentiment
    }

    @Data
    @Builder
    public static class RiskAssessment {
        private List<Risk> strategicRisks;
        private List<Risk> technicalRisks;
        private List<Risk> operationalRisks;
        private List<Risk> financialRisks;
        private double overallRiskScore; // 0-100, lower is better
    }

    @Data
    @Builder
    public static class Risk {
        private String category;
        private String description;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private double probability; // 0-1
        private double impact; // 0-100
        private List<String> mitigationStrategies;
    }
}

