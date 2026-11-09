CREATE TABLE notifications (
    event_id           uuid        NOT NULL REFERENCES domain_events (id),
    person_id          uuid        NOT NULL REFERENCES people (id),
    type               text        NOT NULL,
    expedition_id      uuid        NOT NULL,
    sent_at            timestamptz NOT NULL,
    registration_order bigint      GENERATED ALWAYS AS IDENTITY UNIQUE,
    PRIMARY KEY (event_id, person_id)
);

CREATE INDEX notifications_inbox_idx ON notifications (person_id, registration_order DESC);
