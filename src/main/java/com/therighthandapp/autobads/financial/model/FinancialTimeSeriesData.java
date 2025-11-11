package com.therighthandapp.autobads.financial.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Training data record for financial forecasting
 * Represents historical financial time series data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialTimeSeriesData {
    
    private String industry;
    private String productCategory;
    private LocalDate startDate;
    
    // Historical data points (monthly)
    private List<BigDecimal> revenue;
    private List<BigDecimal> costs;
    private List<BigDecimal> profit;
    private List<BigDecimal> marketSizeGrowth;
    private List<Integer> competitorCount;
    private List<Double> customerAcquisitionCost;
    
    // Metadata
    private String region;
    private String businessModel; // SaaS, Marketplace, E-commerce, etc.
    private int employeeCount;
    
    /**
     * Validate data integrity
     */
    public boolean isValid() {
        return revenue != null && !revenue.isEmpty() 
            && revenue.size() >= 12  // At least 1 year of data
            && costs != null && costs.size() == revenue.size()
            && profit != null && profit.size() == revenue.size();
    }
    
    /**
     * Get sequence length for LSTM
     */
    public int getSequenceLength() {
        return revenue != null ? revenue.size() : 0;
    }
}
