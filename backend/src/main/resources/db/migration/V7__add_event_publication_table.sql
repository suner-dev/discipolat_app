-- Table required by Spring Modulith for reliable event publication
-- Ensures transactional event publishing with completion tracking
CREATE TABLE IF NOT EXISTS event_publication (
    id UUID PRIMARY KEY,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    parent_id UUID,
    completion_date TIMESTAMP WITH TIME ZONE,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    listener_id VARCHAR(512) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_publication_completion_date
    ON event_publication (completion_date)
    WHERE completion_date IS NULL;
