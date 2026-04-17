package com.therighthandapp.autobads.prompts;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all LLM prompts used in Auto-BADS
 * Provides versioning, caching, and prompt management
 */
@Slf4j
@Component
public class PromptRegistry {

    private final Map<String, PromptTemplate> prompts = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing prompt registry with enhanced prompts");
        
        // Register all agent prompts
        registerIdeationPrompts();
        registerRequirementsPrompts();
        registerProductAnalysisPrompts();
        registerFinancialAnalysisPrompts();
        registerMarketAnalysisPrompts();
        registerSolutionSynthesisPrompts();
        registerIntegrationPrompts();
        
        log.info("Loaded {} prompt templates", prompts.size());
    }
    
    /**
     * Get prompt by ID
     */
    public PromptTemplate getPrompt(String id) {
        PromptTemplate prompt = prompts.get(id);
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt not found: " + id);
        }
        return prompt;
    }
    
    /**
     * Get prompt by ID and version
     */
    public PromptTemplate getPrompt(String id, String version) {
        String key = id + ":" + version;
        PromptTemplate prompt = prompts.get(key);
        if (prompt == null) {
            // Fall back to latest version
            return getPrompt(id);
        }
        return prompt;
    }
    
    /**
     * Register new prompt
     */
    public void registerPrompt(PromptTemplate prompt) {
        prompts.put(prompt.getId(), prompt);
        log.debug("Registered prompt: {} v{}", prompt.getName(), prompt.getVersion());
    }
    
    // =================================================================================
    // 1. IDEATION AGENT PROMPTS
    // =================================================================================
    
    private void registerIdeationPrompts() {
        // Problem Statement Translation
        PromptTemplate problemStatement = PromptTemplate.builder()
            .id("ideation.problem-statement")
            .name("Problem Statement Translation")
            .version("2.0")
            .description("Transforms unstructured ideas into structured problem statements")
            .template("""
                You are an expert business analyst specializing in requirements elicitation with 15+ years of experience.
                
                Transform the following unstructured business idea into a clear, structured problem statement using the 5W1H framework.
                
                **Framework:**
                - WHO: Target users/customers (demographics, psychographics, pain points)
                - WHAT: Core problem or unmet need (be specific, quantify if possible)
                - WHY: Impact and urgency (business value, market gap)
                - WHERE: Context and environment (industry, geography, platform)
                - WHEN: Frequency and timing (how often, critical moments)
                - HOW: Current workarounds and alternatives (competitors, manual processes)
                
                **Raw Idea:** {rawIdea}
                
                **Output Requirements:**
                - Maximum 1500 characters total
                - 2-3 concise paragraphs covering all 5W1H elements
                - Data-driven where possible (market size, TAM, pain severity)
                - Avoid jargon; use clear business language
                - Include quantifiable metrics
                - DO NOT include examples or sample outputs
                
                Provide ONLY the structured problem statement without any preamble or examples:
                """)
            .requiredVariables(List.of("rawIdea"))
            .examples(Arrays.asList(
                PromptTemplate.FewShotExample.builder()
                    .input("Build an app for finding local restaurants")
                    .expectedOutput("""
                        **WHO**: Urban millennials (25-40) and food enthusiasts who dine out 3-5x weekly and struggle with decision fatigue when choosing restaurants.
                        
                        **WHAT**: Users spend 15-20 minutes browsing multiple apps/sites to find restaurants that match their current mood, dietary needs, and social context. 60% report dissatisfaction with generic recommendations.
                        
                        **WHY**: $863B global restaurant industry with 47% of consumers relying on mobile apps for discovery. Personalized recommendations can increase user engagement by 3x and reduce decision time by 70%.
                        
                        **WHERE**: Metropolitan areas (1M+ population) with high restaurant density (50+ options/mile). Initial focus: San Francisco, NYC, Chicago.
                        
                        **WHEN**: Peak decision moments: lunch (11am-1pm), dinner (6-8pm), weekend brunch. 73% of searches happen within 1 hour of intended dining time.
                        
                        **HOW**: Currently using Yelp (generic reviews), Google Maps (basic search), Instagram (inspiration but no booking), or asking friends (limited scale). Average uses 3.2 different tools per restaurant decision.
                        """)
                    .explanation("Structured with data, specific user persona, quantified pain points")
                    .build(),
                PromptTemplate.FewShotExample.builder()
                    .input("AI tool for email management")
                    .expectedOutput("""
                        **WHO**: Knowledge workers (executives, managers, consultants) processing 100+ emails daily, spending 28% of workweek on email (McKinsey data), experiencing inbox anxiety.
                        
                        **WHAT**: Inefficient email triage leads to 23 minutes average response time for urgent messages, 4.5 hours weekly spent on low-priority emails, and critical items buried in clutter.
                        
                        **WHY**: $1.8T annual productivity loss from email inefficiency (Radicati). AI-powered prioritization can reduce email processing time by 40% and improve response SLAs by 60%.
                        
                        **WHERE**: Enterprise and SMB environments with high email volume. Industries: Tech (priority), Finance, Consulting, Healthcare.
                        
                        **WHEN**: Continuous problem throughout workday, peak frustration: Monday mornings (200+ weekend emails), post-vacation (500+ backlog), end-of-quarter crunch.
                        
                        **HOW**: Manual rules/filters (rigid, high maintenance), inbox zero methodology (unsustainable), dedicated email time blocks (interrupts urgent items), virtual assistants (expensive $40-80/hr).
                        """)
                    .explanation("Quantifies productivity impact, shows market opportunity, identifies current alternatives")
                    .build()
            ))
            .validation(PromptTemplate.ValidationRules.builder()
                .minLength(300)
                .maxLength(2000)
                .mustContain(Arrays.asList("WHO", "WHAT", "WHY", "WHERE", "WHEN", "HOW"))
                .outputFormat("MARKDOWN")
                .build())
            .metadata(Map.of("agent", "Ideation", "criticality", "HIGH"))
            .createdAt(LocalDateTime.now())
            .build();
        
        registerPrompt(problemStatement);
        
        // Business Hypothesis Generation
        PromptTemplate businessHypothesis = PromptTemplate.builder()
            .id("ideation.business-hypothesis")
            .name("Business Hypothesis Generation")
            .version("2.0")
            .description("Creates testable business hypotheses from problem statements")
            .template("""
                Based on the following problem statement, generate a testable business hypothesis using the Lean Startup methodology.
                
                **Problem Statement:** {problemStatement}
                
                **Hypothesis Format:**
                "We believe that [specific solution] will result in [measurable outcome] for [target segment], 
                which we can validate by measuring [specific metrics] with a success criteria of [threshold]."
                
                **Requirements:**
                - Solution must be specific and actionable
                - Outcome must be quantifiable (not "improve" but "increase X by Y%")
                - Metrics must be SMART (Specific, Measurable, Achievable, Relevant, Time-bound)
                - Success threshold must be data-driven (industry benchmarks or competitor data)
                
                **Additionally provide:**
                - Riskiest assumption: What must be true for this to work?
                - Falsification criteria: What would prove this wrong?
                - Minimum viable test: Smallest experiment to validate
                
                Generate the business hypothesis:
                """)
            .requiredVariables(List.of("problemStatement"))
            .examples(Collections.singletonList(
                PromptTemplate.FewShotExample.builder()
                    .input("Problem: Knowledge workers spending 28% of workweek on email triage...")
                    .expectedOutput("""
                        **Hypothesis:**
                        We believe that an AI-powered email prioritization system will reduce email processing time by 40% for knowledge workers processing 100+ emails daily, which we can validate by measuring:
                        - Primary: Email triage time reduction (target: 40% reduction from 28% to 17% of workweek = 3.5 hours saved)
                        - Secondary: Response time for urgent emails (target: <5 min, down from 23 min avg)
                        - Tertiary: User satisfaction score (target: NPS >50)
                        Success criteria: 80% of beta users achieve >30% time reduction within 4 weeks.
                        
                        **Riskiest Assumption:**
                        Users will trust AI to accurately classify email urgency without human verification.
                        
                        **Falsification Criteria:**
                        - <20% time reduction after 4 weeks of use
                        - >15% false positive rate on urgent email classification
                        - <60% user activation rate (daily active users)
                        
                        **Minimum Viable Test:**
                        2-week pilot with 50 users, Gmail plugin, track time-to-inbox-zero, classification accuracy, measure manual override rate
                        """)
                    .explanation("SMART metrics, falsifiable, clear MVP scope")
                    .build()
            ))
            .validation(PromptTemplate.ValidationRules.builder()
                .minLength(200)
                .mustContain(Arrays.asList("We believe", "validate by measuring"))
                .build())
            .createdAt(LocalDateTime.now())
            .build();
        
        registerPrompt(businessHypothesis);
    }
    
    // =================================================================================
    // 2. REQUIREMENTS AGENT PROMPTS (continued in next method for readability)
    // =================================================================================
    
    private void registerRequirementsPrompts() {
        PromptTemplate functionalRequirements = PromptTemplate.builder()
            .id("requirements.functional")
            .name("Functional Requirements Extraction")
            .version("2.0")
            .description("Extracts detailed functional requirements from problem statements")
            .template("""
                You are a senior requirements engineer. Extract functional requirements from the problem statement.
                
                **Problem Statement:** {problemStatement}
                
                **Output Format (JSON):**
                ```json
                {
                  "requirements": [
                    {
                      "id": "FR-001",
                      "category": "Authentication|Core|Integration|UI|Reporting",
                      "requirement": "Detailed requirement description",
                      "priority": "MUST|SHOULD|COULD|WONT",
                      "acceptance_criteria": ["criterion 1", "criterion 2"],
                      "dependencies": ["FR-002"],
                      "estimated_complexity": "LOW|MEDIUM|HIGH"
                    }
                  ]
                }
                ```
                
                **Guidelines:**
                - Use MoSCoW prioritization (MUST/SHOULD/COULD/WONT)
                - Each requirement must be testable and verifiable
                - Include 3-5 acceptance criteria per requirement
                - Map dependencies between requirements
                - Estimate complexity based on technical challenges
                
                Generate 10-15 functional requirements:
                """)
            .requiredVariables(List.of("problemStatement"))
            .validation(PromptTemplate.ValidationRules.builder()
                .mustContain(Arrays.asList("FR-", "MUST", "acceptance_criteria"))
                .outputFormat("JSON")
                .requireStructuredData(true)
                .build())
            .createdAt(LocalDateTime.now())
            .build();
        
        registerPrompt(functionalRequirements);
    }
    
    // Continue with remaining agent prompts...
    private void registerProductAnalysisPrompts() {
        registerPrompt(EnhancedPromptDefinitions.innovationAssessmentPrompt());
    }
    
    private void registerFinancialAnalysisPrompts() {
        registerPrompt(EnhancedPromptDefinitions.tcoCalculationPrompt());
    }
    
    private void registerMarketAnalysisPrompts() {
        registerPrompt(EnhancedPromptDefinitions.swotAnalysisPrompt());
    }
    
    private void registerSolutionSynthesisPrompts() {
        registerPrompt(EnhancedPromptDefinitions.buildSolutionArchitecturePrompt());
    }
    
    private void registerIntegrationPrompts() {
        registerPrompt(EnhancedPromptDefinitions.srsGenerationPrompt());
    }
    
    /**
     * Get all prompts for a specific agent
     */
    public List<PromptTemplate> getPromptsByAgent(String agentName) {
        return prompts.values().stream()
            .filter(p -> agentName.equals(p.getMetadata().get("agent")))
            .toList();
    }
    
    /**
     * Get all prompts
     */
    public Collection<PromptTemplate> getAllPrompts() {
        return prompts.values();
    }
}
