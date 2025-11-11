package com.therighthandapp.autobads.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.therighthandapp.autobads.config.TestKafkaConfig;
import com.therighthandapp.autobads.core.domain.BusinessIdea;
import com.therighthandapp.autobads.core.domain.Status;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import com.therighthandapp.autobads.ingestion.SemanticTranslationAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API integration tests.
 * Tests HTTP endpoints for idea submission and solution retrieval.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestKafkaConfig.class)
@Transactional
class RestApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessIdeaRepository repository;

    @MockBean
    private SemanticTranslationAgent semanticAgent;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        
        // Mock semantic agent
        when(semanticAgent.translateToStructuredProblem(anyString()))
            .thenReturn("""
                Problem: Structured problem statement
                
                Current Situation:
                - Market opportunity identified
                - Customer pain points validated
                
                Desired Outcome:
                - Scalable solution
                - Market validation
                
                Success Criteria:
                - Customer satisfaction > 80%
                - ROI within 12 months
                """);
        
        when(semanticAgent.generateBusinessHypothesis(anyString()))
            .thenReturn("Business hypothesis: Strong market potential with achievable development timeline.");
    }

    @Test
    void testSubmitIdeaEndpoint() throws Exception {
        // Given: A raw business idea
        Map<String, String> request = Map.of(
            "idea", "Build AI-powered customer service chatbot for e-commerce"
        );

        // When: POST to /api/v1/ideas
        mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                
                // Then: Should return 200 with idea ID
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ideaId").exists())
                .andExpect(jsonPath("$.status").value("INGESTION_IN_PROGRESS"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testSubmitEmptyIdea() throws Exception {
        // Given: Empty idea
        Map<String, String> request = Map.of("idea", "");

        // When: POST empty idea
        mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Idea cannot be empty"));
    }

    @Test
    void testSubmitNullIdea() throws Exception {
        // Given: Null idea
        Map<String, String> request = Map.of();

        // When: POST without idea field
        mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Idea cannot be empty"));
    }

    @Test
    void testGetIdeaById() throws Exception {
        // Given: A saved business idea
        BusinessIdea idea = BusinessIdea.builder()
                .rawIdea("Build SaaS platform")
                .structuredProblemStatement("Structured problem")
                .status(Status.ANALYZING)
                .submittedAt(Instant.now())
                .build();
        idea = repository.save(idea);

        // When: GET /api/v1/ideas/{ideaId}
        mockMvc.perform(get("/api/v1/ideas/" + idea.getId()))
                
                // Then: Should return idea details
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ideaId").value(idea.getId().toString()))
                .andExpect(jsonPath("$.status").value("ANALYZING"))
                .andExpect(jsonPath("$.rawIdea").value("Build SaaS platform"))
                .andExpect(jsonPath("$.structuredProblem").value("Structured problem"))
                .andExpect(jsonPath("$.submittedAt").exists());
    }

    @Test
    void testGetNonExistentIdea() throws Exception {
        // Given: Random UUID that doesn't exist
        UUID randomId = UUID.randomUUID();

        // When: GET non-existent idea
        mockMvc.perform(get("/api/v1/ideas/" + randomId))
                
                // Then: Should return 404
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetSolutionForNonExistentIdea() throws Exception {
        // Given: Random UUID
        UUID randomId = UUID.randomUUID();

        // When: GET /api/v1/solutions/{ideaId}
        mockMvc.perform(get("/api/v1/solutions/" + randomId))
                
                // Then: Should return IN_PROGRESS status
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCompleteIdeaSubmissionWorkflow() throws Exception {
        // Given: Submit a new idea via API
        Map<String, String> request = Map.of(
            "idea", "Create blockchain-based supply chain tracking system"
        );

        // When: POST idea
        String response = mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then: Extract idea ID and verify it's saved
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        String ideaId = (String) responseMap.get("ideaId");
        assertThat(ideaId).isNotNull();

        // And: Verify idea is retrievable
        mockMvc.perform(get("/api/v1/ideas/" + ideaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ideaId").value(ideaId))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.rawIdea").value("Create blockchain-based supply chain tracking system"));
    }

    @Test
    void testMultipleIdeasSubmission() throws Exception {
        // Given: Multiple different ideas
        String idea1 = "AI-powered fitness coaching app";
        String idea2 = "Virtual reality training platform";
        String idea3 = "IoT smart home automation";

        // When: Submit all ideas
        String id1 = submitIdeaAndGetId(idea1);
        String id2 = submitIdeaAndGetId(idea2);
        String id3 = submitIdeaAndGetId(idea3);

        // Then: All should have unique IDs
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);

        // And: All should be retrievable
        mockMvc.perform(get("/api/v1/ideas/" + id1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawIdea").value(idea1));

        mockMvc.perform(get("/api/v1/ideas/" + id2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawIdea").value(idea2));

        mockMvc.perform(get("/api/v1/ideas/" + id3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawIdea").value(idea3));
    }

    @Test
    void testInvalidJsonRequest() throws Exception {
        // Given: Invalid JSON
        String invalidJson = "{invalid json";

        // When: POST invalid JSON
        mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                
                // Then: Should return 400
                .andExpect(status().isBadRequest());
    }

    @Test
    void testContentTypeValidation() throws Exception {
        // Given: Valid request but wrong content type
        // When: POST with wrong content type
        mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.TEXT_PLAIN)
                .content("idea=Test idea"))
                
                // Then: Should return 415 Unsupported Media Type
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testCorsHeaders() throws Exception {
        // Given: Cross-origin request
        Map<String, String> request = Map.of("idea", "CORS test idea");

        // When: POST with Origin header
        mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", "http://localhost:3000")
                .content(objectMapper.writeValueAsString(request)))
                
                // Then: Should succeed (CORS configured)
                .andExpect(status().isOk());
    }

    @Test
    void testLargeIdeaSubmission() throws Exception {
        // Given: Very large idea text (simulating edge case)
        String largeIdea = "A".repeat(5000); // 5000 characters
        Map<String, String> request = Map.of("idea", largeIdea);

        // When: POST large idea
        mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                
                // Then: Should accept it
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ideaId").exists());
    }

    @Test
    void testSpecialCharactersInIdea() throws Exception {
        // Given: Idea with special characters
        String specialIdea = "Build platform with émojis 🚀 and spëcial çhars: <>&\"'";
        Map<String, String> request = Map.of("idea", specialIdea);

        // When: POST idea with special chars
        String response = mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then: Should handle special characters correctly
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        String ideaId = (String) responseMap.get("ideaId");

        // Verify it's saved correctly
        BusinessIdea savedIdea = repository.findById(UUID.fromString(ideaId)).orElseThrow();
        assertThat(savedIdea.getRawIdea()).isEqualTo(specialIdea);
    }

    /**
     * Helper method to submit idea and extract ID
     */
    @SuppressWarnings("unchecked")
    private String submitIdeaAndGetId(String idea) throws Exception {
        Map<String, String> request = Map.of("idea", idea);
        
        String response = mockMvc.perform(post("/api/v1/ideas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        return (String) responseMap.get("ideaId");
    }
}
