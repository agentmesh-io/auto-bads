package com.therighthandapp.autobads.financial.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SyntheticDataGenerator.
 * Tests the generation of synthetic financial time series data for ML training.
 */
class SyntheticDataGeneratorTest {

    private SyntheticDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SyntheticDataGenerator();
    }

    @Test
    void testGenerateTrainingData() {
        // When: Generate 10 samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(10);

        // Then: Should generate exactly 10 samples
        assertThat(data).hasSize(10);
    }

    @Test
    void testGeneratedDataHasCorrectMonthsOfData() {
        // When: Generate samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(5);

        // Then: Each sample should have 36-60 months of data
        data.forEach(sample -> {
            assertThat(sample.getRevenue().size()).isBetween(36, 60);
            assertThat(sample.getCosts()).hasSameSizeAs(sample.getRevenue());
            assertThat(sample.getProfit()).hasSameSizeAs(sample.getRevenue());
            assertThat(sample.getMarketSizeGrowth()).hasSameSizeAs(sample.getRevenue());
            assertThat(sample.getCompetitorCount()).hasSameSizeAs(sample.getRevenue());
            assertThat(sample.getCustomerAcquisitionCost()).hasSameSizeAs(sample.getRevenue());
        });
    }

    @Test
    void testGeneratedDataHasValidIndustry() {
        // When: Generate samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(20);

        // Then: All samples should have valid industries
        List<String> validIndustries = List.of(
            "SaaS", "E-commerce", "Fintech", "HealthTech", 
            "EdTech", "MarketPlace", "AI/ML Services", "Cybersecurity"
        );

        data.forEach(sample -> {
            assertThat(sample.getIndustry()).isIn(validIndustries);
        });
    }

    @Test
    void testRevenueIsPositive() {
        // When: Generate samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(10);

        // Then: All revenue values should be positive
        data.forEach(sample -> {
            sample.getRevenue().forEach(revenue -> {
                assertThat(revenue).isGreaterThan(BigDecimal.ZERO);
            });
        });
    }

    @Test
    void testCostsArePositive() {
        // When: Generate samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(10);

        // Then: All cost values should be positive
        data.forEach(sample -> {
            sample.getCosts().forEach(cost -> {
                assertThat(cost).isGreaterThan(BigDecimal.ZERO);
            });
        });
    }

    @Test
    void testProfitCalculation() {
        // When: Generate samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(5);

        // Then: Profit should be revenue - costs (within rounding tolerance)
        data.forEach(sample -> {
            for (int i = 0; i < sample.getRevenue().size(); i++) {
                BigDecimal revenue = sample.getRevenue().get(i);
                BigDecimal costs = sample.getCosts().get(i);
                BigDecimal profit = sample.getProfit().get(i);
                BigDecimal expectedProfit = revenue.subtract(costs);
                
                // Allow small rounding differences (up to 1 cent)
                BigDecimal diff = profit.subtract(expectedProfit).abs();
                assertThat(diff).isLessThanOrEqualTo(new BigDecimal("0.01"));
            }
        });
    }

    @Test
    void testMarketGrowthRateIsRealistic() {
        // When: Generate samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(10);

        // Then: Market growth rates should be between -50% and 200% (reasonable range)
        data.forEach(sample -> {
            sample.getMarketSizeGrowth().forEach(rate -> {
                assertThat(rate).isGreaterThan(new BigDecimal("-0.5"));
                assertThat(rate).isLessThan(new BigDecimal("2.0"));
            });
        });
    }

    @Test
    void testCompetitorCountIsRealistic() {
        // When: Generate samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(10);

        // Then: Competitor count should be between 1 and 100
        data.forEach(sample -> {
            sample.getCompetitorCount().forEach(count -> {
                assertThat(count).isGreaterThanOrEqualTo(1);
                assertThat(count).isLessThanOrEqualTo(100);
            });
        });
    }

    @Test
    void testCustomerAcquisitionCostIsPositive() {
        // When: Generate samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(10);

        // Then: CAC should be positive
        data.forEach(sample -> {
            sample.getCustomerAcquisitionCost().forEach(cac -> {
                assertThat(cac).isGreaterThan(0.0);
            });
        });
    }

    @Test
    void testDataShowsGrowthPatterns() {
        // When: Generate many samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(50);

        // Then: At least some samples should show revenue growth
        long growingSamples = data.stream()
            .filter(sample -> {
                BigDecimal firstRevenue = sample.getRevenue().get(0);
                BigDecimal lastRevenue = sample.getRevenue().get(sample.getRevenue().size() - 1);
                return lastRevenue.compareTo(firstRevenue) > 0;
            })
            .count();

        assertThat(growingSamples).isGreaterThan(40); // Most should grow
    }

    @Test
    void testDataVariety() {
        // When: Generate many samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(100);

        // Then: Should have variety in industries
        long uniqueIndustries = data.stream()
            .map(FinancialTimeSeriesData::getIndustry)
            .distinct()
            .count();

        assertThat(uniqueIndustries).isGreaterThan(5); // Should use most industries
    }

    @Test
    void testEmptyDataHandling() {
        // When: Generate zero samples
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(0);

        // Then: Should return empty list
        assertThat(data).isEmpty();
    }

    @Test
    void testLargeDatasetGeneration() {
        // When: Generate large dataset
        List<FinancialTimeSeriesData> data = generator.generateTrainingData(500);

        // Then: Should generate all 500 samples
        assertThat(data).hasSize(500);
        
        // And all should be valid with 36-60 months
        data.forEach(sample -> {
            assertThat(sample.getIndustry()).isNotNull();
            assertThat(sample.getRevenue().size()).isBetween(36, 60);
        });
    }
}
