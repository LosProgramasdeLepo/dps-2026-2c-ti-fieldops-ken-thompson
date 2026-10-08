CREATE TABLE expedition_runs (
    expedition_id uuid PRIMARY KEY REFERENCES expeditions (id),
    status        text NOT NULL CHECK (status IN ('IN_PROGRESS', 'SUSPENDED', 'FINISHED'))
);

CREATE TABLE run_activities (
    expedition_id uuid        NOT NULL REFERENCES expedition_runs (expedition_id) ON DELETE CASCADE,
    position      integer     NOT NULL,
    activity_id   uuid        NOT NULL,
    started_at    timestamptz NOT NULL,
    finished_at   timestamptz,
    result        text,
    PRIMARY KEY (expedition_id, position),
    UNIQUE (expedition_id, activity_id) DEFERRABLE INITIALLY DEFERRED,
    CHECK ((finished_at IS NULL) = (result IS NULL)),
    CHECK (finished_at >= started_at)
);

CREATE TABLE run_incidents (
    expedition_id uuid        NOT NULL REFERENCES expedition_runs (expedition_id) ON DELETE CASCADE,
    position      integer     NOT NULL,
    description   text        NOT NULL,
    occurred_at   timestamptz NOT NULL,
    activity_id   uuid,
    PRIMARY KEY (expedition_id, position)
);

CREATE TABLE run_observations (
    expedition_id uuid        NOT NULL REFERENCES expedition_runs (expedition_id) ON DELETE CASCADE,
    position      integer     NOT NULL,
    note          text        NOT NULL,
    recorded_at   timestamptz NOT NULL,
    PRIMARY KEY (expedition_id, position)
);
