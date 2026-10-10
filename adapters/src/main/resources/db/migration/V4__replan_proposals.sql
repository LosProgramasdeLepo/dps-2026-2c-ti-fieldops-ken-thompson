CREATE TABLE replan_proposals (
    id                   uuid PRIMARY KEY,
    original_id          uuid        NOT NULL REFERENCES expeditions (id),
    suggested_id         uuid        NOT NULL UNIQUE REFERENCES expeditions (id) DEFERRABLE INITIALLY DEFERRED,
    incident_description text        NOT NULL,
    incident_at          timestamptz NOT NULL,
    incident_activity_id uuid        NOT NULL,
    decision             text        NOT NULL CHECK (decision IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    decided_by           uuid REFERENCES people (id),
    decided_at           timestamptz,
    registration_order   bigint GENERATED ALWAYS AS IDENTITY UNIQUE,
    CHECK ((decision = 'PENDING') = (decided_by IS NULL)),
    CHECK ((decided_by IS NULL) = (decided_at IS NULL))
);
