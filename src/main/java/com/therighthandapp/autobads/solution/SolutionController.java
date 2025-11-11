package com.therighthandapp.autobads.solution;

import com.therighthandapp.autobads.core.domain.SolutionPackage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Solution REST Controller - Provides access to solution recommendations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/solutions")
public class SolutionController {

    private final ConcurrentHashMap<UUID, SolutionResult> solutionCache = new ConcurrentHashMap<>();

    @EventListener
    public void onSolutionCompleted(com.therighthandapp.autobads.core.events.SolutionRecommendationCompletedEvent event) {
        log.info("Caching solution for idea: {}", event.getIdeaId());
        solutionCache.put(event.getIdeaId(), new SolutionResult(
                event.getAllSolutionPackages(),
                event.getRecommendedSolution(),
                event.getRecommendation()
        ));
    }

    @GetMapping("/{ideaId}")
    public ResponseEntity<?> getSolution(@PathVariable UUID ideaId) {
        SolutionResult result = solutionCache.get(ideaId);

        if (result == null) {
            return ResponseEntity.ok(Map.of(
                    "status", "IN_PROGRESS",
                    "message", "Solution synthesis in progress. Please check back shortly."
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ideaId", ideaId);
        response.put("recommended", convertToResponse(result.recommended));
        response.put("alternatives", result.allSolutions.stream()
                .filter(s -> !s.getPackageId().equals(result.recommended.getPackageId()))
                .map(this::convertToResponse)
                .toList());
        response.put("recommendation", result.recommendation);
        response.put("status", "COMPLETED");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ideaId}/packages/{packageId}")
    public ResponseEntity<?> getSolutionPackageDetails(
            @PathVariable UUID ideaId,
            @PathVariable String packageId) {

        SolutionResult result = solutionCache.get(ideaId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return result.allSolutions.stream()
                .filter(s -> s.getPackageId().equals(packageId))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> convertToResponse(SolutionPackage solution) {
        Map<String, Object> map = new HashMap<>();
        map.put("packageId", solution.getPackageId());
        map.put("type", solution.getType());
        map.put("description", solution.getDescription());
        map.put("estimatedWeeks", solution.getTimeline().getEstimatedWeeks());
        map.put("requiredDevelopers", solution.getResources().getRequiredDevelopers());
        map.put("score", solution.getScore());
        return map;
    }

    private record SolutionResult(
            List<SolutionPackage> allSolutions,
            SolutionPackage recommended,
            String recommendation
    ) {}
}
