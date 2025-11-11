package com.therighthandapp.autobads.solution;

import com.therighthandapp.autobads.core.domain.SolutionPackage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Recommendation Engine - Analyzes and ranks Build/Buy/Hybrid solutions
 * Uses multi-criteria decision analysis to select optimal approach
 */
@Slf4j
@Component
public class RecommendationEngine {

    public SolutionPackage selectBestSolution(List<SolutionPackage> solutions, SynthesisContext context) {
        log.info("Analyzing {} solution alternatives for idea: {}", 
                solutions.size(), context.getIdeaEvent().getIdeaId());
        
        // Multi-criteria decision analysis
        // Weights: Strategic Alignment (30%), Technical Feasibility (30%), 
        //          Market Opportunity (25%), Resource Cost (15%)
        
        return solutions.stream()
                .peek(solution -> log.debug("Solution {}: Score = {}", 
                    solution.getType(), solution.getScore().getWeightedTotalScore()))
                .max(Comparator.comparingDouble(s -> 
                    calculateWeightedScore(s.getScore())))
                .orElseThrow(() -> new IllegalStateException("No solutions available"));
    }
    
    private double calculateWeightedScore(SolutionPackage.SolutionScore score) {
        return (score.getStrategicAlignment() * 0.30) +
               (score.getTechnicalFeasibility() * 0.30) +
               (score.getMarketOpportunity() * 0.25) -
               (score.getResourceCost() * 0.15); // Subtract cost (lower is better)
    }
    
    // Convenience method that combines selection and rationale generation
    public String generateRecommendation(SolutionPackage recommended, SynthesisContext context) {
        log.info("Generating recommendation rationale for solution: {}", recommended.getType());
        return generateRecommendationRationale(recommended, java.util.Collections.emptyList());
    }
    
    public String generateRecommendationRationale(SolutionPackage best, List<SolutionPackage> alternatives) {
        StringBuilder rationale = new StringBuilder();
        rationale.append(String.format("Recommended Solution: %s%n%n", best.getType()));
        rationale.append(String.format("Score: %.2f%n", best.getScore().getWeightedTotalScore()));
        rationale.append(String.format("Rationale: %s%n%n", best.getScore().getRecommendation()));
        
        rationale.append("Alternative Analysis:%n");
        alternatives.stream()
                .filter(alt -> !alt.equals(best))
                .forEach(alt -> rationale.append(String.format("- %s (Score: %.2f): %s%n", 
                        alt.getType(), 
                        alt.getScore().getWeightedTotalScore(),
                        alt.getScore().getRecommendation())));
        
        return rationale.toString();
    }
}
