ALTER TABLE run_incidents ADD COLUMN id uuid;
UPDATE run_incidents SET id = gen_random_uuid();
ALTER TABLE run_incidents ALTER COLUMN id SET NOT NULL;
ALTER TABLE run_incidents ADD CONSTRAINT run_incidents_id_key UNIQUE (id);

-- A proposal embeds the incident that triggered it. It keeps the id of the matching run incident when there is one.
ALTER TABLE replan_proposals ADD COLUMN incident_id uuid;
UPDATE replan_proposals proposal
SET incident_id = COALESCE(
    (SELECT incident.id
     FROM run_incidents incident
     WHERE incident.description = proposal.incident_description
       AND incident.occurred_at = proposal.incident_at
       AND incident.activity_id = proposal.incident_activity_id
     ORDER BY incident.expedition_id, incident.position
     LIMIT 1),
    gen_random_uuid()
);
ALTER TABLE replan_proposals ALTER COLUMN incident_id SET NOT NULL;
