package com.therighthandapp.autobads.core.events;

import com.therighthandapp.autobads.core.domain.ProductAnalysisResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when product analysis is completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalysisCompletedEvent {
    private UUID ideaId;
    private ProductAnalysisResult analysisResult;
    private LocalDateTime timestamp;
    
    // Legacy getter for backward compatibility
    public ProductAnalysisResult getAnalysis() {
        return analysisResult;
    }
}

