-- Increase serialized_event column size to support larger event payloads
-- Spring's event_publication table needs more than 255 chars for event JSON
ALTER TABLE event_publication ALTER COLUMN serialized_event TYPE VARCHAR(2000);
