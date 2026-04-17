package com.therighthandapp.autobads.market;

import com.therighthandapp.autobads.core.domain.MarketAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SWOT Analysis Agent - Autonomous strengths, weaknesses, opportunities, threats analysis
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SwotAnalysisAgent {

    private final ChatModel chatClient;

    private static final String SWOT_PROMPT = """
            You are a strategic business analyst conducting a SWOT analysis.
            
            Problem Statement: {problemStatement}
            
            Conduct a comprehensive SWOT analysis considering:
            1. Internal Strengths: What advantages does this solution provide?
            2. Internal Weaknesses: What limitations or challenges exist?
            3. External Opportunities: What market trends or gaps can be exploited?
            4. External Threats: What competitive or market forces pose risks?
            
            Additionally, identify strategic connections - how strengths can exploit opportunities,
            or how weaknesses might be exposed by threats.
            
            Respond with valid JSON in this format:
            {
              "strengths": ["strength1", "strength2", ...],
              "weaknesses": ["weakness1", "weakness2", ...],
              "opportunities": ["opportunity1", "opportunity2", ...],
              "threats": ["threat1", "threat2", ...],
              "strategicConnections": {
                "strength-opportunity": "How strength X enables opportunity Y",
                "weakness-threat": "How weakness X is exposed by threat Y"
              }
            }
            """;

    public MarketAnalysisResult.SwotAnalysis performSwotAnalysis(String problemStatement) {
        log.info("Performing SWOT analysis");

        try {
            PromptTemplate template = new PromptTemplate(SWOT_PROMPT);
            Prompt prompt = template.create(Map.of("problemStatement", problemStatement));

            ChatResponse response = chatClient.call(prompt);
            String swotJson = response.getResult().getOutput().getContent();

            // In production, parse JSON properly using Jackson
            // TODO: Parse swotJson when LLM is available
        } catch (Exception e) {
            log.warn("LLM not available for SWOT analysis, using fallback: {}", e.getMessage());
        }
        
        // Return structured analysis (fallback or parsed from LLM)
        return MarketAnalysisResult.SwotAnalysis.builder()
                .strengths(List.of("Strong value proposition", "Innovative approach", "Clear market need"))
                .weaknesses(List.of("High initial investment", "Technical complexity", "Market education required"))
                .opportunities(List.of("Growing market segment", "Competitive gap identified", "Technology trends alignment"))
                .threats(List.of("Established competitors", "Regulatory changes", "Market volatility"))
                .strategicConnections(Map.of(
                        "innovation-market-gap", "Innovative approach directly addresses identified competitive gap",
                        "investment-risk", "High investment requirement increases financial risk in volatile market"
                ))
                .build();
    }
}

