package com.therighthandapp.autobads.solution;

import com.therighthandapp.autobads.core.domain.FinancialAnalysisResult;
import com.therighthandapp.autobads.core.domain.MarketAnalysisResult;
import com.therighthandapp.autobads.core.domain.ProductAnalysisResult;
import com.therighthandapp.autobads.core.events.IdeaIngestedEvent;
import lombok.Data;

/**
 * Context accumulator for solution synthesis
 * Collects all analysis results for generating Build vs Buy vs Partner recommendations
 */
@Data
public class SynthesisContext {
    private final IdeaIngestedEvent ideaEvent;
    private MarketAnalysisResult marketAnalysis;
    private ProductAnalysisResult productAnalysis;
    private FinancialAnalysisResult financialAnalysis;

    public SynthesisContext(IdeaIngestedEvent ideaEvent) {
        this.ideaEvent = ideaEvent;
    }

    public boolean isReadyForSynthesis() {
        return marketAnalysis != null && 
               productAnalysis != null && 
               financialAnalysis != null;
    }
}
