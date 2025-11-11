package com.therighthandapp.autobads.financial.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates synthetic financial time series data for model training
 * 
 * Simulates realistic startup/product revenue patterns:
 * - Various industries (SaaS, E-commerce, Fintech, etc.)
 * - Different growth trajectories (hockey stick, linear, plateau)
 * - Seasonal patterns
 * - Market dynamics (competition, growth rate)
 */
@Slf4j
@Component
public class SyntheticDataGenerator {

    private final Random random = new Random(42);
    
    private static final String[] INDUSTRIES = {
        "SaaS", "E-commerce", "Fintech", "HealthTech", "EdTech", 
        "MarketPlace", "AI/ML Services", "Cybersecurity"
    };
    
    private static final String[] PRODUCT_CATEGORIES = {
        "B2B Platform", "B2C App", "Developer Tools", "Enterprise Software",
        "Consumer Services", "Analytics Platform", "Automation Tools"
    };
    
    private static final String[] BUSINESS_MODELS = {
        "Subscription", "Transaction-based", "Freemium", "Usage-based", "License"
    };
    
    /**
     * Generate training dataset with diverse scenarios
     */
    public List<FinancialTimeSeriesData> generateTrainingData(int numSamples) {
        log.info("Generating {} synthetic financial time series samples", numSamples);
        
        List<FinancialTimeSeriesData> dataset = new ArrayList<>();
        
        for (int i = 0; i < numSamples; i++) {
            GrowthPattern pattern = GrowthPattern.values()[random.nextInt(GrowthPattern.values().length)];
            dataset.add(generateTimeSeries(pattern, 36 + random.nextInt(25))); // 36-60 months
        }
        
        return dataset;
    }
    
    /**
     * Generate single time series with specified growth pattern
     */
    private FinancialTimeSeriesData generateTimeSeries(GrowthPattern pattern, int months) {
        String industry = INDUSTRIES[random.nextInt(INDUSTRIES.length)];
        String productCategory = PRODUCT_CATEGORIES[random.nextInt(PRODUCT_CATEGORIES.length)];
        String businessModel = BUSINESS_MODELS[random.nextInt(BUSINESS_MODELS.length)];
        
        List<BigDecimal> revenue = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        List<BigDecimal> profit = new ArrayList<>();
        List<BigDecimal> marketGrowth = new ArrayList<>();
        List<Integer> competitorCount = new ArrayList<>();
        List<Double> cac = new ArrayList<>();
        
        // Initial conditions
        double baseRevenue = 10000 + random.nextDouble() * 90000; // $10K-$100K
        double baseCost = baseRevenue * (0.7 + random.nextDouble() * 0.2); // 70-90% cost ratio
        int initialCompetitors = 5 + random.nextInt(20);
        double baseCac = 50 + random.nextDouble() * 450; // $50-$500
        
        for (int month = 0; month < months; month++) {
            // Apply growth pattern
            double growthFactor = calculateGrowthFactor(pattern, month, months);
            
            // Add seasonality (e.g., Q4 boost, summer dip)
            double seasonalFactor = 1.0 + 0.1 * Math.sin(2 * Math.PI * month / 12.0);
            
            // Revenue with noise
            double monthRevenue = baseRevenue * growthFactor * seasonalFactor * (0.9 + random.nextDouble() * 0.2);
            revenue.add(BigDecimal.valueOf(monthRevenue).setScale(2, RoundingMode.HALF_UP));
            
            // Costs (improve efficiency over time)
            double costRatio = Math.max(0.4, 0.8 - (month / (double) months) * 0.2);
            double monthCost = monthRevenue * costRatio * (0.95 + random.nextDouble() * 0.1);
            costs.add(BigDecimal.valueOf(monthCost).setScale(2, RoundingMode.HALF_UP));
            
            // Profit
            profit.add(BigDecimal.valueOf(monthRevenue - monthCost).setScale(2, RoundingMode.HALF_UP));
            
            // Market growth rate (%) - decreases as market matures
            double marketGrowthRate = 0.05 + 0.15 * Math.exp(-month / 24.0) + (random.nextDouble() - 0.5) * 0.02;
            marketGrowth.add(BigDecimal.valueOf(marketGrowthRate).setScale(4, RoundingMode.HALF_UP));
            
            // Competitor count increases over time
            int competitors = initialCompetitors + (int) (month * 0.5 + random.nextInt(3));
            competitorCount.add(competitors);
            
            // CAC trend (decreases with scale, marketing optimization)
            double monthCac = baseCac * (1.0 - month / (double) months * 0.3) * (0.9 + random.nextDouble() * 0.2);
            cac.add(monthCac);
        }
        
        return FinancialTimeSeriesData.builder()
            .industry(industry)
            .productCategory(productCategory)
            .businessModel(businessModel)
            .startDate(LocalDate.now().minusMonths(months))
            .revenue(revenue)
            .costs(costs)
            .profit(profit)
            .marketSizeGrowth(marketGrowth)
            .competitorCount(competitorCount)
            .customerAcquisitionCost(cac)
            .region("North America")
            .employeeCount(10 + random.nextInt(90))
            .build();
    }
    
    /**
     * Calculate growth factor based on pattern
     */
    private double calculateGrowthFactor(GrowthPattern pattern, int month, int totalMonths) {
        double t = month / (double) totalMonths;
        
        return switch (pattern) {
            case HOCKEY_STICK -> {
                // Slow start, then exponential growth
                if (t < 0.4) {
                    yield 1.0 + t * 0.5; // Slow growth
                } else {
                    yield 1.2 + Math.pow((t - 0.4) * 5, 1.8); // Exponential
                }
            }
            case LINEAR -> 1.0 + t * 2.5; // Steady linear growth
            case S_CURVE -> {
                // Logistic growth (fast middle, plateau at end)
                yield 3.0 / (1 + Math.exp(-10 * (t - 0.5)));
            }
            case EXPONENTIAL -> Math.pow(1.08, month); // 8% monthly growth
            case PLATEAU -> {
                // Fast early growth, then plateau
                if (t < 0.6) {
                    yield 1.0 + Math.pow(t * 3, 1.5);
                } else {
                    yield 2.5 + (random.nextDouble() - 0.5) * 0.1; // Flat with noise
                }
            }
        };
    }
    
    private enum GrowthPattern {
        HOCKEY_STICK,  // Typical VC-backed startup
        LINEAR,        // Steady growth
        S_CURVE,       // Market penetration curve
        EXPONENTIAL,   // Viral/network effects
        PLATEAU        // Early traction, then market saturation
    }
}
