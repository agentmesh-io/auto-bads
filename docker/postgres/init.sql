-- Auto-BADS PostgreSQL Initialization Script

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS autobads;

-- Set default schema
SET search_path TO autobads, public;

-- Create business_ideas table
CREATE TABLE IF NOT EXISTS business_ideas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    raw_idea TEXT NOT NULL,
    structured_problem_statement TEXT,
    status VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('SUBMITTED', 'INGESTION_IN_PROGRESS', 'ANALYSIS_IN_PROGRESS', 'SOLUTION_SYNTHESIS_IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_business_ideas_status ON business_ideas(status);
CREATE INDEX IF NOT EXISTS idx_business_ideas_submitted_at ON business_ideas(submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_business_ideas_raw_idea_trgm ON business_ideas USING gin(raw_idea gin_trgm_ops);

-- Create analysis results table (for caching)
CREATE TABLE IF NOT EXISTS analysis_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idea_id UUID NOT NULL REFERENCES business_ideas(id) ON DELETE CASCADE,
    analysis_type VARCHAR(50) NOT NULL,
    result_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_analysis_type CHECK (analysis_type IN ('MARKET', 'PRODUCT', 'FINANCIAL'))
);

CREATE INDEX IF NOT EXISTS idx_analysis_results_idea_id ON analysis_results(idea_id);
CREATE INDEX IF NOT EXISTS idx_analysis_results_type ON analysis_results(analysis_type);

-- Create solution packages table
CREATE TABLE IF NOT EXISTS solution_packages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idea_id UUID NOT NULL REFERENCES business_ideas(id) ON DELETE CASCADE,
    package_type VARCHAR(20) NOT NULL,
    package_data JSONB NOT NULL,
    score DECIMAL(5,2),
    is_recommended BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_package_type CHECK (package_type IN ('BUILD', 'BUY', 'HYBRID'))
);

CREATE INDEX IF NOT EXISTS idx_solution_packages_idea_id ON solution_packages(idea_id);
CREATE INDEX IF NOT EXISTS idx_solution_packages_recommended ON solution_packages(is_recommended) WHERE is_recommended = TRUE;

-- Create audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    idea_id UUID REFERENCES business_ideas(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_log_idea_id ON audit_log(idea_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA autobads TO autobads;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA autobads TO autobads;

-- Insert sample data (optional for testing)
-- INSERT INTO business_ideas (raw_idea, status)
-- VALUES ('A mobile app for language learning through AI conversations', 'SUBMITTED');

COMMENT ON TABLE business_ideas IS 'Core table storing submitted business ideas and their processing status';
COMMENT ON TABLE analysis_results IS 'Cache table for market, product, and financial analysis results';
COMMENT ON TABLE solution_packages IS 'Stores generated Build/Buy/Hybrid solution packages';
COMMENT ON TABLE audit_log IS 'Audit trail for all events in the system';

