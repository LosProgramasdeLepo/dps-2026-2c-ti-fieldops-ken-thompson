CREATE TABLE domain_events (
    id             uuid        PRIMARY KEY,
    sequence       bigint      GENERATED ALWAYS AS IDENTITY UNIQUE,
    type           text        NOT NULL,
    schema_version int         NOT NULL CHECK (schema_version > 0),
    expedition_id  uuid        NOT NULL,
    occurred_at    timestamptz NOT NULL,
    recorded_at    timestamptz NOT NULL,
    payload        jsonb       NOT NULL
);

CREATE INDEX domain_events_expedition_sequence_idx ON domain_events (expedition_id, sequence);
CREATE INDEX domain_events_type_sequence_idx ON domain_events (type, sequence);

-- The log only grows: an event is a fact, so it is never rewritten or removed. There is no foreign key to
-- expeditions on purpose, because the log is history and must not depend on the order of writes to other tables.
CREATE FUNCTION reject_domain_event_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'domain_events is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER domain_events_append_only
    BEFORE UPDATE OR DELETE ON domain_events
    FOR EACH ROW EXECUTE FUNCTION reject_domain_event_change();

CREATE TRIGGER domain_events_no_truncate
    BEFORE TRUNCATE ON domain_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_domain_event_change();
