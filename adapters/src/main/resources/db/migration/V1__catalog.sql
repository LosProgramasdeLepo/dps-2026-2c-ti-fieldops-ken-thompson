CREATE TABLE certifications (
    id                 uuid PRIMARY KEY,
    name               text NOT NULL,
    registration_order bigint GENERATED ALWAYS AS IDENTITY UNIQUE
);

CREATE TABLE people (
    id                 uuid PRIMARY KEY,
    name               text NOT NULL,
    registration_order bigint GENERATED ALWAYS AS IDENTITY UNIQUE
);

CREATE TABLE person_certifications (
    person_id        uuid    NOT NULL REFERENCES people (id) ON DELETE CASCADE,
    position         integer NOT NULL,
    certification_id uuid    NOT NULL REFERENCES certifications (id),
    PRIMARY KEY (person_id, position),
    UNIQUE (person_id, certification_id)
);

CREATE TABLE person_availability (
    person_id    uuid        NOT NULL REFERENCES people (id) ON DELETE CASCADE,
    position     integer     NOT NULL,
    period_start timestamptz NOT NULL,
    period_end   timestamptz NOT NULL,
    PRIMARY KEY (person_id, position),
    CHECK (period_end >= period_start)
);

CREATE TABLE vehicles (
    id                 uuid PRIMARY KEY,
    capacity           integer NOT NULL CHECK (capacity >= 0),
    registration_order bigint GENERATED ALWAYS AS IDENTITY UNIQUE
);

CREATE TABLE vehicle_availability (
    vehicle_id   uuid        NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    position     integer     NOT NULL,
    period_start timestamptz NOT NULL,
    period_end   timestamptz NOT NULL,
    PRIMARY KEY (vehicle_id, position),
    CHECK (period_end >= period_start)
);

CREATE TABLE instruments (
    id                 uuid PRIMARY KEY,
    kind               text NOT NULL,
    registration_order bigint GENERATED ALWAYS AS IDENTITY UNIQUE
);

CREATE TABLE instrument_availability (
    instrument_id uuid        NOT NULL REFERENCES instruments (id) ON DELETE CASCADE,
    position      integer     NOT NULL,
    period_start  timestamptz NOT NULL,
    period_end    timestamptz NOT NULL,
    PRIMARY KEY (instrument_id, position),
    CHECK (period_end >= period_start)
);

CREATE TABLE consumables (
    id                 uuid PRIMARY KEY,
    name               text    NOT NULL,
    stock              integer NOT NULL CHECK (stock >= 0),
    registration_order bigint GENERATED ALWAYS AS IDENTITY UNIQUE
);

CREATE TABLE permits (
    id                 uuid PRIMARY KEY,
    zone               text        NOT NULL,
    valid_from         timestamptz NOT NULL,
    valid_to           timestamptz NOT NULL,
    kind               text        NOT NULL,
    registration_order bigint GENERATED ALWAYS AS IDENTITY UNIQUE,
    CHECK (valid_to >= valid_from)
);
