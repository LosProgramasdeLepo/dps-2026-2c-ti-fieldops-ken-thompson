-- The backfill of V6 could give two proposals the same incident: only the oldest one keeps the id.
UPDATE replan_proposals proposal
SET incident_id = gen_random_uuid()
FROM (
    SELECT id, row_number() OVER (PARTITION BY incident_id ORDER BY registration_order) AS position
    FROM replan_proposals
) ranked
WHERE ranked.id = proposal.id
  AND ranked.position > 1;

ALTER TABLE replan_proposals ADD CONSTRAINT replan_proposals_incident_id_key UNIQUE (incident_id);

CREATE TABLE event_fanout (
    event_id      uuid        PRIMARY KEY REFERENCES domain_events (id),
    dispatched_at timestamptz NOT NULL
);

CREATE TABLE event_deliveries (
    event_id        uuid        NOT NULL REFERENCES domain_events (id),
    consumer        text        NOT NULL,
    status          text        NOT NULL CHECK (status IN ('PENDING', 'RETRYING', 'DELIVERED', 'FAILED')),
    attempts        int         NOT NULL CHECK (attempts >= 0),
    next_attempt_at timestamptz,
    last_attempt_at timestamptz,
    last_error      text,
    delivered_at    timestamptz,
    PRIMARY KEY (event_id, consumer),
    CHECK ((status = 'DELIVERED') = (delivered_at IS NOT NULL)),
    CHECK ((status IN ('PENDING', 'RETRYING')) = (next_attempt_at IS NOT NULL))
);

CREATE INDEX event_deliveries_due_idx ON event_deliveries (next_attempt_at)
    WHERE status IN ('PENDING', 'RETRYING');

-- Events recorded before this migration were already processed synchronously, so nothing delivers them again.
INSERT INTO event_fanout (event_id, dispatched_at)
SELECT id, now() FROM domain_events;
