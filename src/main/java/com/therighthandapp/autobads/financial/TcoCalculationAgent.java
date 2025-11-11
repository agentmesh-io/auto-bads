package com.therighthandapp.autobads.financial;

import com.therighthandapp.autobads.core.domain.FinancialAnalysisResult.TotalCostOfOwnership;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Agent responsible for calculating Total Cost of Ownership (TCO)
 * Analyzes initial investment, operational costs, maintenance, licensing, and resource costs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TcoCalculationAgent {
    private final ChatClient chatClient;

    public TotalCostOfOwnership calculateTCO(String ideaDescription, String industry) {
        log.info("Calculating TCO for idea in {} industry", industry);
        
        String prompt = String.format("""
                Analyze the Total Cost of Ownership for this business idea:
                
                Idea: %s
                Industry: %s
                
                Provide detailed cost breakdown for:
                1. Initial Investment (development, infrastructure, licenses)
                2. Yearly Operational Cost (hosting, cloud services, utilities)
                3. Maintenance Cost (updates, bug fixes, support)
                4. Third Party Licensing (APIs, software licenses, subscriptions)
                5. Internal Resource Cost (salaries, training, overhead)
                
                Return JSON format with numeric values for 5-year projection.
                """, ideaDescription, industry);
        
        // TODO: In production, use chatClient to parse LLM response to structured data
        // For now, return sample data structure
        return TotalCostOfOwnership.builder()
                .initialInvestment(new BigDecimal("150000"))
                .yearlyOperationalCost(new BigDecimal("75000"))
                .maintenanceCost(new BigDecimal("25000"))
                .thirdPartyLicensing(new BigDecimal("15000"))
                .internalResourceCost(new BigDecimal("250000"))
                .totalFiveYearTco(new BigDecimal("625000"))
                .build();
    }
}
