-- V233__discipleship_core.sql
-- ============================================================
-- DISCIPLESHIP (parcours de discipleship, étapes, progression,
-- assignations mentor, réunions, rapports)
-- Plan : docs/SPEC_BACKEND_SERVICES_MOBILES_V233.md
--
-- Le mobile (mobile/lib/features/discipleship) appelle ces endpoints
-- depuis longtemps ; le backend n'existait pas. Cette migration crée
-- les tables additivement, sans toucher à l'existant.
--
-- Conventions alignées sur V222..V232 : TIMESTAMPTZ, now(), FK
-- explicites, CHECK sur les énumérations, ON DELETE explicite.
-- IDs : BIGSERIAL (le contrat mobile utilise des int, pas des UUID).
-- ============================================================

CREATE TABLE IF NOT EXISTS discipleship_journeys (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    type            VARCHAR(20) NOT NULL DEFAULT 'CUSTOM'
        CHECK (type IN ('NEW_BELIEVER','GROWTH','LEADERSHIP','MINISTRY','CUSTOM')),
    total_stages    INT NOT NULL DEFAULT 0,
    start_date      TIMESTAMPTZ NOT NULL DEFAULT now(),
    end_date        TIMESTAMPTZ,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS discipleship_stages (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    journey_id      BIGINT NOT NULL REFERENCES discipleship_journeys(id) ON DELETE CASCADE,
    "order"         INT NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    color           VARCHAR(20),
    icon            VARCHAR(50),
    duration_days   INT,
    is_optional     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS discipleship_stage_requirements (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    stage_id        BIGINT NOT NULL REFERENCES discipleship_stages(id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL
        CHECK (type IN ('ATTEND_MEETING','COMPLETE_STUDY','MEMORIZE_VERSE','PRACTICE_HABIT','SERVE','SHARE_TESTIMONY','READ_BOOK','COMPLETE_COURSE','ATTEND_EVENT','CUSTOM')),
    description     TEXT,
    reference_id    VARCHAR(100),
    reference_name  VARCHAR(200),
    is_required     BOOLEAN NOT NULL DEFAULT TRUE,
    "order"         INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS discipleship_stage_rewards (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    stage_id        BIGINT NOT NULL REFERENCES discipleship_stages(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL
        CHECK (type IN ('BADGE','CERTIFICATE','POINTS','ITEM','PRIVILEGE','RECOGNITION')),
    name            VARCHAR(200),
    description     TEXT,
    icon            VARCHAR(50),
    image_url       TEXT,
    points          INT,
    badge_id        VARCHAR(100),
    badge_name      VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS disciple_progress (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    disciple_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journey_id              BIGINT NOT NULL REFERENCES discipleship_journeys(id) ON DELETE CASCADE,
    current_stage_id        BIGINT REFERENCES discipleship_stages(id) ON DELETE SET NULL,
    completed_stages        INT NOT NULL DEFAULT 0,
    total_stages            INT NOT NULL DEFAULT 0,
    completed_requirements  INT NOT NULL DEFAULT 0,
    total_requirements      INT NOT NULL DEFAULT 0,
    started_at              TIMESTAMPTZ,
    last_activity_at        TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    status                  VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (status IN ('NOT_STARTED','IN_PROGRESS','STALLED','COMPLETED','ABANDONED')),
    next_milestone_date     TIMESTAMPTZ,
    next_milestone_name     VARCHAR(200),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS disciple_progress_requirements (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    progress_id     BIGINT NOT NULL REFERENCES disciple_progress(id) ON DELETE CASCADE,
    requirement_id  BIGINT NOT NULL REFERENCES discipleship_stage_requirements(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','VERIFIED','WAIVED')),
    completed_at    TIMESTAMPTZ,
    evidence        TEXT,
    notes           TEXT,
    verified_by     UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS mentor_assignments (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    mentor_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    disciple_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journey_id          BIGINT NOT NULL REFERENCES discipleship_journeys(id) ON DELETE CASCADE,
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at            TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','ACTIVE','ENDED','CANCELLED')),
    notes               TEXT,
    meeting_frequency_days INT NOT NULL DEFAULT 7,
    last_meeting_at     TIMESTAMPTZ,
    next_meeting_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS mentor_meetings (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    assignment_id       BIGINT NOT NULL REFERENCES mentor_assignments(id) ON DELETE CASCADE,
    mentor_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    disciple_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheduled_at        TIMESTAMPTZ NOT NULL,
    actual_at           TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED','RESCHEDULED')),
    notes               TEXT,
    action_items        TEXT,
    next_steps          TEXT,
    duration_minutes    INT,
    location            VARCHAR(200),
    is_group            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_disc_journeys_tenant ON discipleship_journeys (tenant_id);
CREATE INDEX IF NOT EXISTS idx_disc_stages_journey ON discipleship_stages (tenant_id, journey_id, "order");
CREATE INDEX IF NOT EXISTS idx_disc_progress_tenant ON disciple_progress (tenant_id);
CREATE INDEX IF NOT EXISTS idx_disc_progress_disciple ON disciple_progress (tenant_id, disciple_id);
CREATE INDEX IF NOT EXISTS idx_disc_assign_tenant ON mentor_assignments (tenant_id);
CREATE INDEX IF NOT EXISTS idx_disc_meetings_tenant ON mentor_meetings (tenant_id);
CREATE INDEX IF NOT EXISTS idx_disc_meetings_assignment ON mentor_meetings (tenant_id, assignment_id);

COMMENT ON TABLE discipleship_journeys IS 'Parcours de discipleship (V233).';
COMMENT ON TABLE discipleship_stages IS 'Étapes d''un parcours (V233).';
COMMENT ON TABLE disciple_progress IS 'Progression d''un disciple dans un parcours (V233).';
COMMENT ON TABLE mentor_assignments IS 'Assignation mentor-disciple (V233).';
COMMENT ON TABLE mentor_meetings IS 'Réunions mentor-disciple (V233).';
