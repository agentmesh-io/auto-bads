package com.therighthandapp.autobads.financial;

import com.therighthandapp.autobads.core.domain.MarketAnalysisResult;
import com.therighthandapp.autobads.core.domain.ProductAnalysisResult;
import com.therighthandapp.autobads.core.events.IdeaIngestedEvent;
import lombok.Data;

/**
 * Context accumulator for multi-agent collaboration
 */
@Data
public class AnalysisContext {
    private final IdeaIngestedEvent ideaEvent;
    private MarketAnalysisResult marketAnalysis;
    private ProductAnalysisResult productAnalysis;

    public AnalysisContext(IdeaIngestedEvent ideaEvent) {
        this.ideaEvent = ideaEvent;
    }

    public boolean isReadyForFinancialAnalysis() {
        return marketAnalysis != null && productAnalysis != null;
    }
}
/**
 * Financial Analysis Module - Phase IV: Financial Viability and Risk Assessment
 *
 * This module implements the Analytical Agent from the Discovery Triad.
 * Features hybrid LLM/Deep Learning architecture for financial forecasting.
 *
 * Key responsibilities:
 * - Total Cost of Ownership (TCO) calculation
 * - NPV and ROI forecasting using DL models (LSTM/GRU/Transformer)
 * - Risk assessment and mitigation strategies
 * - Explainable AI (XAI) for financial predictions
 * - Market sentiment analysis using LLM
 */
