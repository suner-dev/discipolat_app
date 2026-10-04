-- ============================================================
-- SPEC_ONBOARDING_FLOWS (BE-4) — annonces publiques modérées.
--
-- D4 : workflow DRAFT → PENDING_MODERATION → PUBLISHED / REJECTED,
-- EXPIRED posé par le job quotidien. Visible sur le landing page
-- via GET /api/v1/public/announcements (PUBLISHED non expirées).
-- ============================================================

CREATE TABLE IF NOT EXISTS public_announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    org_node_id UUID,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    image_url VARCHAR(500),
    city VARCHAR(120),
    country VARCHAR(120),
    event_at TIMESTAMPTZ,
    link_url VARCHAR(500),
    access_ref VARCHAR(64),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING_MODERATION', 'PUBLISHED', 'REJECTED', 'EXPIRED')),
    moderation_note VARCHAR(500),
    moderated_by UUID,
    moderated_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_public_announcements_status
    ON public_announcements(status, event_at DESC);
CREATE INDEX IF NOT EXISTS idx_public_announcements_tenant
    ON public_announcements(tenant_id, status);

COMMENT ON TABLE public_announcements IS
    'SPEC_ONBOARDING_FLOWS : annonces/pub d''une église visibles sur le landing après modération';
