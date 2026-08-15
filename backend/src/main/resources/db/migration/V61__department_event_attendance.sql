-- Présence des membres du département à un événement rattaché au département.
-- Une ligne par (département, événement, âme) ; présent/absent marqué par un
-- responsable, un chef de famille ou le faiseur de l'âme.
CREATE TABLE department_event_attendance (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    soul_id     UUID NOT NULL REFERENCES souls(id) ON DELETE CASCADE,
    present     BOOLEAN NOT NULL,
    marked_by   UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_dept_event_soul UNIQUE (department_id, event_id, soul_id)
);

CREATE INDEX idx_dept_event_attendance_event ON department_event_attendance (event_id);
CREATE INDEX idx_dept_event_attendance_soul ON department_event_attendance (soul_id);
