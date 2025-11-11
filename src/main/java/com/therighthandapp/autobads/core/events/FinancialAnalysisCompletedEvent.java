package com.therighthandapp.autobads.core.events;

import com.therighthandapp.autobads.core.domain.FinancialAnalysisResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when financial analysis is completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialAnalysisCompletedEvent {
    private UUID ideaId;
    private FinancialAnalysisResult analysisResult;
    private LocalDateTime timestamp;
    
    // Legacy getter for backward compatibility
    public FinancialAnalysisResult getAnalysis() {
        return analysisResult;
    }
}

