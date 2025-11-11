-- H2 schema customizations for test database
-- This runs after Hibernate creates the schema but before tests execute

-- Increase event_publication.serialized_event column from VARCHAR(255) to VARCHAR(2000)
-- This allows larger event payloads in performance tests
ALTER TABLE event_publication ALTER COLUMN serialized_event VARCHAR(2000);
