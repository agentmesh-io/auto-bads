package com.therighthandapp.autobads.core.events;

import com.therighthandapp.autobads.core.domain.SolutionPackage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when solution recommendation is completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionRecommendationCompletedEvent {
    private UUID ideaId;
    private List<SolutionPackage> allSolutionPackages;
    private SolutionPackage recommendedSolution;
    private String recommendation;
    private LocalDateTime timestamp;
}
