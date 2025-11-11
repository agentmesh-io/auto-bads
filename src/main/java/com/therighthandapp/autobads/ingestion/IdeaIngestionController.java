package com.therighthandapp.autobads.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API for idea ingestion
 */
@RestController
@RequestMapping("/api/v1/ideas")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class IdeaIngestionController {

    private static final Logger log = LoggerFactory.getLogger(IdeaIngestionController.class);

    private final IdeaIngestionService ingestionService;

    public IdeaIngestionController(IdeaIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitIdea(@RequestBody Map<String, String> request) {
        String rawIdea = request.get("idea");

        if (rawIdea == null || rawIdea.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Idea cannot be empty"));
        }

        log.info("Received idea submission request");
        UUID ideaId = ingestionService.ingestIdea(rawIdea);

        return ResponseEntity.ok(Map.of(
                "ideaId", ideaId.toString(),
                "status", "INGESTION_IN_PROGRESS",
                "message", "Your idea has been submitted and is being analyzed by the Auto-BADS system"
        ));
    }

    @GetMapping("/{ideaId}")
    public ResponseEntity<?> getIdea(@PathVariable UUID ideaId) {
        try {
            var idea = ingestionService.getIdea(ideaId);
            // Use HashMap to allow null values (Map.of() doesn't allow nulls)
            var response = new java.util.HashMap<String, Object>();
            response.put("ideaId", idea.getId());
            response.put("status", idea.getStatus());
            response.put("submittedAt", idea.getSubmittedAt());
            response.put("rawIdea", idea.getRawIdea());
            response.put("structuredProblem", idea.getStructuredProblemStatement() != null
                    ? idea.getStructuredProblemStatement()
                    : "Processing...");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

