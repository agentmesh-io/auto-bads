package com.therighthandapp.autobads.core.domain;

/**
 * Status of a business idea through the analysis pipeline
 */
public enum Status {
    /**
     * Initial state - idea has been submitted
     */
    SUBMITTED,
    
    /**
     * Analysis in progress (product, financial, market analysis)
     */
    ANALYZING,
    
    /**
     * Solution synthesis in progress
     */
    SOLUTION_SYNTHESIS_IN_PROGRESS,
    
    /**
     * All analysis completed successfully
     */
    COMPLETED,
    
    /**
     * Analysis failed due to errors
     */
    FAILED
}
