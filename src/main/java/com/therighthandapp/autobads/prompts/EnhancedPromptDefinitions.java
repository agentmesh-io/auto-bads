package com.therighthandapp.autobads.prompts;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Enhanced prompts for Product, Financial, Market, Solution, and Integration agents
 */
@Component
public class EnhancedPromptDefinitions {

    // =================================================================================
    // PRODUCT ANALYSIS PROMPTS
    // =================================================================================
    
    public static PromptTemplate innovationAssessmentPrompt() {
        return PromptTemplate.builder()
            .id("product.innovation-assessment")
            .name("Innovation Assessment")
            .version("2.1")
            .description("Evaluates innovation level using multiple frameworks")
            .template("""
                You are an innovation strategist with expertise in Clayton Christensen's disruption theory, TRIZ, and design thinking.
                
                Analyze the innovation potential of this product idea:
                
                **Problem Statement:** {problemStatement}
                
                **Analysis Framework:**
                
                1. **Innovation Classification:**
                   - Incremental (10-30%): Improves existing solutions
                   - Architectural (30-50%): New combination of existing tech
                   - Radical (50-75%): Novel approach, high uncertainty
                   - Disruptive (75-95%): Creates new market or reshapes existing
                
                2. **Evaluation Criteria** (score 0-100 each):
                   - Novelty: How unique is the core idea?
                   - Technical Feasibility: Can it be built with current tech?
                   - Market Readiness: Is market ready for this?
                   - Competitive Differentiation: How different from alternatives?
                   - Scalability Potential: Can it grow 10x-100x?
                
                3. **Innovation Risks:**
                   - Technical risks (algorithm accuracy, latency, data quality)
                   - Market risks (adoption, switching costs, network effects)
                   - Execution risks (team capability, time-to-market)
                   - Regulatory risks (compliance, data privacy)
                
                **Output (JSON):**
                ```json
                {
                  "innovationLevel": 0-100,
                  "innovationType": "Incremental|Architectural|Radical|Disruptive",
                  "scores": {
                    "novelty": 0-100,
                    "technicalFeasibility": 0-100,
                    "marketReadiness": 0-100,
                    "differentiation": 0-100,
                    "scalability": 0-100
                  },
                  "uniqueValuePropositions": ["uvp1", "uvp2", "uvp3"],
                  "innovationRisks": [
                    {"category": "Technical", "risk": "...", "severity": "LOW|MEDIUM|HIGH|CRITICAL", "mitigation": "..."}
                  ],
                  "claytonChristensenAnalysis": {
                    "isDisruptive": true|false,
                    "targetMarket": "Low-end|New market",
                    "explanation": "..."
                  }
                }
                ```
                
                Provide comprehensive innovation assessment:
                """)
            .requiredVariables(List.of("problemStatement"))
            .examples(Arrays.asList(
                PromptTemplate.FewShotExample.builder()
                    .input("AI-powered email prioritization system")
                    .expectedOutput("""
                        {
                          "innovationLevel": 72,
                          "innovationType": "Radical",
                          "scores": {
                            "novelty": 75,
                            "technicalFeasibility": 85,
                            "marketReadiness": 70,
                            "differentiation": 68,
                            "scalability": 90
                          },
                          "uniqueValuePropositions": [
                            "ML-driven context-aware email classification (not just keyword rules)",
                            "Learns from user behavior to personalize prioritization",
                            "Reduces decision fatigue through intelligent batching"
                          ],
                          "innovationRisks": [
                            {
                              "category": "Technical",
                              "risk": "NLP model accuracy for diverse email types",
                              "severity": "MEDIUM",
                              "mitigation": "Multi-model ensemble, active learning from corrections"
                            },
                            {
                              "category": "Market",
                              "risk": "User trust in AI decision-making for critical communications",
                              "severity": "HIGH",
                              "mitigation": "Transparency dashboard, easy override, gradual feature rollout"
                            }
                          ],
                          "claytonChristensenAnalysis": {
                            "isDisruptive": true,
                            "targetMarket": "New market",
                            "explanation": "Creates new category of 'intelligent inbox' vs traditional email clients that are over-serving on features but under-serving on curation"
                          }
                        }
                        """)
                    .explanation("Comprehensive scoring with risk mitigation strategies")
                    .build()
            ))
            .validation(PromptTemplate.ValidationRules.builder()
                .mustContain(Arrays.asList("innovationLevel", "innovationType", "uniqueValuePropositions"))
                .outputFormat("JSON")
                .build())
            .metadata(Map.of("agent", "Product", "criticality", "HIGH"))
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    // =================================================================================
    // FINANCIAL ANALYSIS PROMPTS
    // =================================================================================
    
    public static PromptTemplate tcoCalculationPrompt() {
        return PromptTemplate.builder()
            .id("financial.tco-calculation")
            .name("Total Cost of Ownership Calculation")
            .version("2.0")
            .description("Calculates 5-year TCO with detailed breakdown")
            .template("""
                You are a financial analyst specializing in TCO modeling for software projects.
                
                Calculate the Total Cost of Ownership (TCO) for this solution:
                
                **Problem Statement:** {problemStatement}
                **Solution Type:** {solutionType}
                
                **Cost Categories:**
                
                1. **Initial Investment** (Year 0):
                   - Development costs (if BUILD): team size × avg salary × duration
                   - Licensing fees (if BUY): per-user/per-seat × quantity
                   - Infrastructure setup: cloud provisioning, CDN, databases
                   - Integration costs: APIs, data migration, training
                
                2. **Yearly Operational Costs** (Years 1-5):
                   - Personnel: DevOps, support, product management
                   - Cloud infrastructure: compute, storage, bandwidth (estimate monthly × 12)
                   - Third-party services: monitoring, analytics, CDN
                   - Support & maintenance: 15-20% of development cost annually
                
                3. **Hidden Costs:**
                   - Technical debt accumulation (5-10% compound annually)
                   - Compliance & security audits
                   - User training and onboarding
                   - Opportunity cost of alternatives
                
                **Output (JSON with detailed breakdown):**
                ```json
                {
                  "initialInvestment": 250000,
                  "yearlyOperationalCost": 120000,
                  "maintenanceCost": 50000,
                  "thirdPartyLicensing": 30000,
                  "internalResourceCost": 180000,
                  "totalFiveYearTco": 1200000,
                  "costBreakdown": {
                    "year0": {"development": 200000, "infrastructure": 50000},
                    "year1": {"operations": 120000, "maintenance": 50000, "licensing": 30000},
                    "year2": {...},
                    "year3": {...},
                    "year4": {...},
                    "year5": {...}
                  },
                  "assumptions": [
                    "Team of 5 engineers @ $120K avg salary",
                    "AWS infrastructure: $8K/month growing 15% YoY",
                    "Maintenance: 20% of initial dev cost"
                  ]
                }
                ```
                
                Calculate TCO with industry-standard assumptions:
                """)
            .requiredVariables(List.of("problemStatement", "solutionType"))
            .validation(PromptTemplate.ValidationRules.builder()
                .mustContain(Arrays.asList("totalFiveYearTco", "costBreakdown", "assumptions"))
                .outputFormat("JSON")
                .build())
            .metadata(Map.of("agent", "Financial", "criticality", "HIGH"))
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    // =================================================================================
    // MARKET ANALYSIS PROMPTS
    // =================================================================================
    
    public static PromptTemplate swotAnalysisPrompt() {
        return PromptTemplate.builder()
            .id("market.swot-analysis")
            .name("SWOT Analysis")
            .version("2.1")
            .description("Comprehensive SWOT with strategic connections")
            .template("""
                You are a strategic business consultant performing SWOT analysis.
                
                **Problem Statement:** {problemStatement}
                
                **SWOT Framework:**
                
                **STRENGTHS** (Internal, Positive):
                - Core competencies and unique capabilities
                - Technology advantages (IP, algorithms, data)
                - Team expertise and track record
                - Resource advantages (funding, partnerships, distribution)
                
                **WEAKNESSES** (Internal, Negative):
                - Resource constraints (capital, talent, time)
                - Technical limitations or dependencies
                - Brand/market awareness gaps
                - Organizational challenges
                
                **OPPORTUNITIES** (External, Positive):
                - Market trends favoring the solution (TAM growth, regulatory changes)
                - Underserved customer segments
                - Technology enablers (cloud, AI, 5G)
                - Partnership or acquisition targets
                
                **THREATS** (External, Negative):
                - Competitive threats (incumbents, new entrants)
                - Market headwinds (economic downturn, budget cuts)
                - Technology risks (obsolescence, better alternatives)
                - Regulatory or compliance risks
                
                **Strategic Connections:**
                - S-O: Leverage strengths to capitalize on opportunities
                - S-T: Use strengths to mitigate threats
                - W-O: Overcome weaknesses by pursuing opportunities
                - W-T: Defend against threats while addressing weaknesses
                
                **Output (JSON):**
                ```json
                {
                  "strengths": ["strength1", "strength2", ...],
                  "weaknesses": ["weakness1", "weakness2", ...],
                  "opportunities": ["opportunity1", "opportunity2", ...],
                  "threats": ["threat1", "threat2", ...],
                  "strategicConnections": {
                    "SO": ["Use AI expertise (S) to capture growing market (O)"],
                    "ST": ["Leverage first-mover advantage (S) to build defensibility before competitors (T)"],
                    "WO": ["Partner with established players (O) to overcome brand weakness (W)"],
                    "WT": ["Acquire competitors (O) to eliminate threat (T) and gain resources"]
                  },
                  "priorityActions": [
                    "Action item based on SWOT insights"
                  ]
                }
                ```
                
                Provide SWOT analysis with strategic recommendations:
                """)
            .requiredVariables(List.of("problemStatement"))
            .validation(PromptTemplate.ValidationRules.builder()
                .mustContain(Arrays.asList("strengths", "weaknesses", "opportunities", "threats", "strategicConnections"))
                .outputFormat("JSON")
                .build())
            .metadata(Map.of("agent", "Market", "criticality", "HIGH"))
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    // =================================================================================
    // SOLUTION SYNTHESIS PROMPTS
    // =================================================================================
    
    public static PromptTemplate buildSolutionArchitecturePrompt() {
        return PromptTemplate.builder()
            .id("solution.build-architecture")
            .name("BUILD Solution Architecture")
            .version("2.0")
            .description("Generates cloud-native architecture for custom build")
            .template("""
                You are a solutions architect designing a production-grade system.
                
                **Requirements:** {requirements}
                **Problem Context:** {problemStatement}
                
                **Design a cloud-native, microservices-based architecture:**
                
                1. **System Architecture:**
                   - High-level architecture diagram (text description)
                   - Microservices breakdown (bounded contexts)
                   - Data flow and event choreography
                   - Deployment topology
                
                2. **Technology Stack:**
                   - Backend: Languages, frameworks (Spring Boot, FastAPI, Node.js)
                   - Frontend: Frameworks (React, Vue, Angular)
                   - Databases: SQL (PostgreSQL), NoSQL (MongoDB, Redis)
                   - Message Queue: Kafka, RabbitMQ, SQS
                   - Infrastructure: Kubernetes, Docker, Terraform
                   - Observability: Prometheus, Grafana, Datadog
                
                3. **Non-Functional Requirements:**
                   - Scalability: Horizontal scaling, auto-scaling
                   - Reliability: Multi-AZ, disaster recovery (RPO/RTO)
                   - Security: Authentication (OAuth2), encryption (TLS), secrets management
                   - Performance: Response times, throughput targets
                
                4. **Integration Points:**
                   - APIs (REST, GraphQL, gRPC)
                   - Event streams (Kafka topics)
                   - Webhooks and callbacks
                
                **Output (JSON):**
                ```json
                {
                  "systemArchitecture": "3-tier microservices with event-driven communication...",
                  "components": [
                    {
                      "name": "API Gateway",
                      "technology": "Kong/AWS API Gateway",
                      "responsibility": "Request routing, rate limiting, authentication"
                    }
                  ],
                  "technologyStack": {
                    "backend": ["Java Spring Boot 3.2", "Python FastAPI"],
                    "frontend": ["React 18", "TypeScript"],
                    "database": ["PostgreSQL 15", "Redis 7"],
                    "infrastructure": ["Kubernetes 1.28", "Terraform", "AWS"]
                  },
                  "integrationPoints": [
                    {"type": "REST API", "pattern": "Sync request/response"},
                    {"type": "Kafka Events", "pattern": "Async event-driven"}
                  ]
                }
                ```
                
                Design the architecture:
                """)
            .requiredVariables(List.of("requirements", "problemStatement"))
            .validation(PromptTemplate.ValidationRules.builder()
                .mustContain(Arrays.asList("systemArchitecture", "components", "technologyStack"))
                .outputFormat("JSON")
                .build())
            .metadata(Map.of("agent", "Solution", "type", "BUILD"))
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    // =================================================================================
    // INTEGRATION PROMPTS (AgentMesh handoff)
    // =================================================================================
    
    public static PromptTemplate srsGenerationPrompt() {
        return PromptTemplate.builder()
            .id("integration.srs-generation")
            .name("Software Requirements Specification")
            .version("2.0")
            .description("Generates IEEE 830-compliant SRS for AgentMesh handoff")
            .template("""
                You are a requirements engineer creating an IEEE 830-compliant Software Requirements Specification.
                
                **Product Context:** {problemStatement}
                **Solution Architecture:** {architecture}
                **Functional Requirements:** {functionalRequirements}
                
                **Generate SRS with the following sections:**
                
                1. **Introduction**
                   - Purpose and scope
                   - Intended audience
                   - Product perspective (context diagram)
                
                2. **Overall Description**
                   - Product functions (high-level capabilities)
                   - User classes and characteristics
                   - Operating environment
                   - Design and implementation constraints
                
                3. **Specific Requirements**
                   - Functional requirements (use cases, user stories)
                   - External interface requirements (UI, API, hardware, communications)
                   - Performance requirements (response time, throughput, capacity)
                   - Security requirements (authentication, authorization, encryption)
                
                4. **Appendices**
                   - Glossary of terms
                   - Analysis models (data flow, state diagrams)
                   - Issues list and open questions
                
                **Output Format: Structured Markdown with clear sections**
                
                Generate comprehensive SRS document:
                """)
            .requiredVariables(List.of("problemStatement", "architecture", "functionalRequirements"))
            .validation(PromptTemplate.ValidationRules.builder()
                .minLength(1000)
                .mustContain(Arrays.asList("Introduction", "Functional Requirements", "Performance Requirements"))
                .outputFormat("MARKDOWN")
                .build())
            .metadata(Map.of("agent", "Integration", "handoffTarget", "AgentMesh"))
            .createdAt(LocalDateTime.now())
            .build();
    }
}
