package com.therighthandapp.autobads.core.events;

import com.therighthandapp.autobads.core.domain.MarketAnalysisResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when market analysis is completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketAnalysisCompletedEvent {
    private UUID ideaId;
    private MarketAnalysisResult analysisResult;
    private LocalDateTime timestamp;
    
    // Legacy getter for backward compatibility
    public MarketAnalysisResult getAnalysis() {
        return analysisResult;
    }
}

