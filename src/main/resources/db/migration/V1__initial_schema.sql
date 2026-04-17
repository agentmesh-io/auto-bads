-- Auto-BADS Initial Schema
-- Version: V1__initial_schema.sql
-- PostgreSQL Database

-- Business Ideas table
CREATE TABLE business_ideas (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    raw_idea TEXT,
    structured_problem_statement TEXT,
    submitted_by VARCHAR(255),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    industry VARCHAR(255),
    target_market VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT business_ideas_status_check CHECK (status IN ('SUBMITTED', 'ANALYZING', 'COMPLETED', 'FAILED', 'SOLUTION_SYNTHESIS_IN_PROGRESS'))
);

-- Business Idea Metadata
CREATE TABLE business_idea_metadata (
    idea_id UUID NOT NULL,
    metadata_key VARCHAR(255) NOT NULL,
    metadata_value VARCHAR(1000),
    PRIMARY KEY (idea_id, metadata_key),
    CONSTRAINT fk_business_idea FOREIGN KEY (idea_id) REFERENCES business_ideas(id) ON DELETE CASCADE
);

-- Analysis Results (Persisted JSON for Product, Financial, Market analysis)
CREATE TABLE analysis_results (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    idea_id UUID NOT NULL,
    analysis_type VARCHAR(50) NOT NULL,
    result_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_idea_analysis FOREIGN KEY (idea_id) REFERENCES business_ideas(id) ON DELETE CASCADE,
    CONSTRAINT analysis_type_check CHECK (analysis_type IN ('PRODUCT', 'FINANCIAL', 'MARKET'))
);

-- Solution Packages
CREATE TABLE solution_packages (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    package_id VARCHAR(255) UNIQUE NOT NULL,
    idea_id UUID NOT NULL,
    solution_type VARCHAR(20) NOT NULL,
    description TEXT,
    solution_data JSONB NOT NULL,
    weighted_score DECIMAL(5,2),
    recommendation TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_idea_solution FOREIGN KEY (idea_id) REFERENCES business_ideas(id) ON DELETE CASCADE,
    CONSTRAINT solution_type_check CHECK (solution_type IN ('BUILD', 'BUY', 'HYBRID'))
);

-- Indexes for performance
CREATE INDEX idx_business_ideas_status ON business_ideas(status);
CREATE INDEX idx_business_ideas_submitted_at ON business_ideas(submitted_at DESC);
CREATE INDEX idx_business_ideas_submitted_by ON business_ideas(submitted_by);
CREATE INDEX idx_analysis_results_idea_id ON analysis_results(idea_id);
CREATE INDEX idx_analysis_results_type ON analysis_results(analysis_type);
CREATE INDEX idx_solution_packages_idea_id ON solution_packages(idea_id);
CREATE INDEX idx_solution_packages_score ON solution_packages(weighted_score DESC);

-- GIN indexes for JSONB queries
CREATE INDEX idx_analysis_results_data ON analysis_results USING GIN(result_data);
CREATE INDEX idx_solution_packages_data ON solution_packages USING GIN(solution_data);

-- Audit trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Audit triggers
CREATE TRIGGER update_business_ideas_updated_at
    BEFORE UPDATE ON business_ideas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_solution_packages_updated_at
    BEFORE UPDATE ON solution_packages
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE business_ideas IS 'Core table storing submitted business ideas for analysis';
COMMENT ON TABLE business_idea_metadata IS 'Flexible metadata storage for business ideas';
COMMENT ON TABLE analysis_results IS 'Stores Product, Financial, and Market analysis results as JSONB';
COMMENT ON TABLE solution_packages IS 'Stores Build/Buy/Hybrid solution alternatives';
COMMENT ON COLUMN analysis_results.result_data IS 'JSONB column containing ProductAnalysisResult, FinancialAnalysisResult, or MarketAnalysisResult';
COMMENT ON COLUMN solution_packages.solution_data IS 'JSONB column containing full SolutionPackage details';


