package com.therighthandapp.autobads.financial;

import com.therighthandapp.autobads.core.domain.FinancialAnalysisResult;
import com.therighthandapp.autobads.core.domain.FinancialAnalysisResult.RiskAssessment;
import com.therighthandapp.autobads.core.domain.MarketAnalysisResult;
import com.therighthandapp.autobads.core.domain.ProductAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Agent responsible for comprehensive risk assessment
 * Analyzes strategic, technical, operational, and financial risks
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskAssessmentAgent {
    private final ChatClient chatClient;

    public RiskAssessment assessRisks(String ideaDescription, 
                                      MarketAnalysisResult marketAnalysis,
                                      ProductAnalysisResult productAnalysis) {
        log.info("Assessing risks for business idea");
        
        String prompt = String.format("""
                Assess comprehensive risks for this business idea:
                
                Idea: %s
                Market Viability: %s
                Product Viability: %s
                
                Analyze and rate (0-100) the following risk categories:
                1. Strategic Risks: Market timing, competitive response, market adoption
                2. Technical Risks: Technology feasibility, scalability challenges, technical debt
                3. Operational Risks: Team capacity, resource availability, execution challenges
                4. Financial Risks: Funding requirements, burn rate, revenue uncertainty
                
                Provide risk score (0-100, where 100 is highest risk) and mitigation strategies.
                """, ideaDescription, 
                marketAnalysis != null ? "Available" : "Pending",
                productAnalysis != null ? "Available" : "Pending");
        
        // TODO: In production, use chatClient to parse LLM response
        return RiskAssessment.builder()
                .overallRiskScore(45.0)
                .strategicRisks(Arrays.asList(
                    createRisk("Strategic", "Market timing uncertainty", "MEDIUM", 0.6, 60),
                    createRisk("Strategic", "Strong incumbent competition", "HIGH", 0.7, 70)
                ))
                .technicalRisks(Arrays.asList(
                    createRisk("Technical", "Scalability at 100k+ users", "MEDIUM", 0.5, 55),
                    createRisk("Technical", "AI model accuracy requirements", "HIGH", 0.6, 65)
                ))
                .operationalRisks(Arrays.asList(
                    createRisk("Operational", "Team hiring challenges", "MEDIUM", 0.5, 50),
                    createRisk("Operational", "Customer support scale", "LOW", 0.4, 40)
                ))
                .financialRisks(Arrays.asList(
                    createRisk("Financial", "Runway until profitability", "HIGH", 0.7, 75),
                    createRisk("Financial", "Customer acquisition cost uncertainty", "MEDIUM", 0.6, 60)
                ))
                .build();
    }
    
    private FinancialAnalysisResult.Risk createRisk(String category, String description, 
                                                      String severity, double probability, double impact) {
        return FinancialAnalysisResult.Risk.builder()
                .category(category)
                .description(description)
                .severity(severity)
                .probability(probability)
                .impact(impact)
                .mitigationStrategies(Arrays.asList(
                    "Monitor continuously",
                    "Develop contingency plans",
                    "Allocate risk budget"
                ))
                .build();
    }
}
