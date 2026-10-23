CREATE TABLE run_activity_consumption (
    expedition_id uuid    NOT NULL REFERENCES expedition_runs (expedition_id) ON DELETE CASCADE,
    activity_id   uuid    NOT NULL,
    consumable_id uuid    NOT NULL REFERENCES consumables (id),
    quantity      integer NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (expedition_id, activity_id, consumable_id)
);

ALTER TABLE vehicles ADD CONSTRAINT vehicles_capacity_positive CHECK (capacity > 0);
ALTER TABLE activity_consumption ADD CONSTRAINT activity_consumption_quantity_positive CHECK (quantity > 0);
ALTER TABLE activities ADD CONSTRAINT activities_duration_positive CHECK (estimated_duration > interval '0');
ALTER TABLE activities ADD CONSTRAINT activities_window_positive CHECK (window_end > window_start);
ALTER TABLE expeditions ADD CONSTRAINT expeditions_period_positive CHECK (period_end > period_start);
ALTER TABLE permits ADD CONSTRAINT permits_validity_positive CHECK (valid_to > valid_from);
ALTER TABLE person_availability ADD CONSTRAINT person_availability_positive CHECK (period_end > period_start);
ALTER TABLE vehicle_availability ADD CONSTRAINT vehicle_availability_positive CHECK (period_end > period_start);
ALTER TABLE instrument_availability ADD CONSTRAINT instrument_availability_positive CHECK (period_end > period_start);
