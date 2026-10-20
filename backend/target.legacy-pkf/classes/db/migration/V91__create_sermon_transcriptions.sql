-- Create sermon_transcriptions table (missing from earlier migration set)
CREATE TABLE IF NOT EXISTS sermon_transcriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    speaker VARCHAR(255),
    title VARCHAR(500) NOT NULL,
    theme VARCHAR(255),
    reference_biblique VARCHAR(255),
    full_text TEXT,
    summary TEXT,
    key_verses TEXT,
    audio_url VARCHAR(1000),
    duration_seconds INTEGER,
    language VARCHAR(10) DEFAULT 'fr',
    transcription_status VARCHAR(20) DEFAULT 'PENDING',
    recorded_at TIMESTAMP,
    transcribed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sermon_transcriptions_tenant_id ON sermon_transcriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sermon_transcriptions_status ON sermon_transcriptions(transcription_status);
