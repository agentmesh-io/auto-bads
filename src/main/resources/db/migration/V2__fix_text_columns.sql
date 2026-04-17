-- Fix column types to handle longer text content
-- Version: V2__fix_text_columns.sql

ALTER TABLE business_ideas 
  ALTER COLUMN title TYPE TEXT,
  ALTER COLUMN description TYPE TEXT,
  ALTER COLUMN raw_idea TYPE TEXT,
  ALTER COLUMN structured_problem_statement TYPE TEXT,
  ALTER COLUMN industry TYPE TEXT,
  ALTER COLUMN target_market TYPE TEXT;
