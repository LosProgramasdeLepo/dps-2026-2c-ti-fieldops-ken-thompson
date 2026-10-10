CREATE TABLE expeditions (
    id            uuid PRIMARY KEY,
    version       integer     NOT NULL CHECK (version >= 1),
    supersedes_id uuid REFERENCES expeditions (id) DEFERRABLE INITIALLY DEFERRED,
    status        text        NOT NULL CHECK (status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'SUPERSEDED')),
    period_start  timestamptz NOT NULL,
    period_end    timestamptz NOT NULL,
    CHECK ((version = 1) = (supersedes_id IS NULL)),
    CHECK (period_end >= period_start)
);

CREATE TABLE registered_expeditions (
    expedition_id      uuid PRIMARY KEY REFERENCES expeditions (id) DEFERRABLE INITIALLY DEFERRED,
    registration_order bigint GENERATED ALWAYS AS IDENTITY UNIQUE
);

CREATE TABLE expedition_objectives (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    description   text    NOT NULL,
    PRIMARY KEY (expedition_id, position)
);

CREATE TABLE expedition_zones (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    name          text    NOT NULL,
    PRIMARY KEY (expedition_id, position)
);

CREATE TABLE expedition_responsibles (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    person_id     uuid    NOT NULL REFERENCES people (id),
    PRIMARY KEY (expedition_id, position)
);

CREATE TABLE expedition_restrictions (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    description   text    NOT NULL,
    PRIMARY KEY (expedition_id, position)
);

CREATE TABLE expedition_permits (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    permit_id     uuid    NOT NULL REFERENCES permits (id),
    PRIMARY KEY (expedition_id, position),
    UNIQUE (expedition_id, permit_id) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE accepted_warnings (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    severity      text    NOT NULL CHECK (severity IN ('WARNING')),
    code          text    NOT NULL,
    message       text    NOT NULL,
    justification text    NOT NULL,
    accepted_by   uuid    NOT NULL REFERENCES people (id),
    PRIMARY KEY (expedition_id, position)
);

CREATE TABLE activities (
    expedition_id      uuid        NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    activity_id        uuid        NOT NULL,
    name               text        NOT NULL,
    estimated_duration interval    NOT NULL,
    risk               text        NOT NULL CHECK (risk IN ('LOW', 'MEDIUM', 'HIGH')),
    zone               text        NOT NULL,
    window_start       timestamptz NOT NULL,
    window_end         timestamptz NOT NULL,
    required_vehicles  integer     NOT NULL CHECK (required_vehicles >= 0),
    PRIMARY KEY (expedition_id, activity_id),
    CHECK (window_end >= window_start)
);

CREATE TABLE itinerary_nodes (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    parent        integer,
    arrangement   text CHECK (arrangement IN ('SEQUENTIAL', 'PARALLEL')),
    activity_id   uuid,
    PRIMARY KEY (expedition_id, position),
    UNIQUE (expedition_id, activity_id) DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (expedition_id, parent) REFERENCES itinerary_nodes (expedition_id, position)
        DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED,
    CHECK ((arrangement IS NULL) <> (activity_id IS NULL)),
    CHECK (parent < position)
);

CREATE TABLE activity_certifications (
    expedition_id    uuid NOT NULL,
    activity_id      uuid NOT NULL,
    certification_id uuid NOT NULL,
    scope            text NOT NULL CHECK (scope IN ('SOMEONE', 'EVERYONE')),
    PRIMARY KEY (expedition_id, activity_id, certification_id, scope),
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE activity_instrument_kinds (
    expedition_id uuid NOT NULL,
    activity_id   uuid NOT NULL,
    kind          text NOT NULL,
    PRIMARY KEY (expedition_id, activity_id, kind),
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE activity_permit_kinds (
    expedition_id uuid NOT NULL,
    activity_id   uuid NOT NULL,
    kind          text NOT NULL,
    PRIMARY KEY (expedition_id, activity_id, kind),
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE activity_consumption (
    expedition_id uuid    NOT NULL,
    activity_id   uuid    NOT NULL,
    consumable_id uuid    NOT NULL,
    quantity      integer NOT NULL CHECK (quantity >= 0),
    PRIMARY KEY (expedition_id, activity_id, consumable_id),
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE activity_predecessors (
    expedition_id  uuid NOT NULL,
    activity_id    uuid NOT NULL,
    predecessor_id uuid NOT NULL,
    PRIMARY KEY (expedition_id, activity_id, predecessor_id),
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (expedition_id, predecessor_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED,
    CHECK (activity_id <> predecessor_id)
);

CREATE TABLE person_assignments (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    activity_id   uuid    NOT NULL,
    person_id     uuid    NOT NULL REFERENCES people (id),
    PRIMARY KEY (expedition_id, position),
    UNIQUE (expedition_id, activity_id, person_id) DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE vehicle_assignments (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    activity_id   uuid    NOT NULL,
    vehicle_id    uuid    NOT NULL REFERENCES vehicles (id),
    PRIMARY KEY (expedition_id, position),
    UNIQUE (expedition_id, activity_id, vehicle_id) DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE instrument_assignments (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    activity_id   uuid    NOT NULL,
    instrument_id uuid    NOT NULL REFERENCES instruments (id),
    PRIMARY KEY (expedition_id, position),
    UNIQUE (expedition_id, activity_id, instrument_id) DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE consumable_assignments (
    expedition_id uuid    NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    position      integer NOT NULL,
    activity_id   uuid    NOT NULL,
    consumable_id uuid    NOT NULL REFERENCES consumables (id),
    quantity      integer NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (expedition_id, position),
    UNIQUE (expedition_id, activity_id, consumable_id) DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (expedition_id, activity_id) REFERENCES activities (expedition_id, activity_id)
        DEFERRABLE INITIALLY DEFERRED
);
