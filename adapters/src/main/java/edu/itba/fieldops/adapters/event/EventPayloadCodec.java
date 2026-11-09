package edu.itba.fieldops.adapters.event;

import edu.itba.fieldops.domain.event.ActivityCompleted;
import edu.itba.fieldops.domain.event.ActivityStarted;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.ReplanDecided;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.event.RunFinished;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.Outcome;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage translation of a domain event, shared by the SQL and MongoDB adapters. The payload only holds JSON-neutral
 * values (strings, numbers and nested maps), so each adapter can persist it as it likes. It is self-contained: it
 * carries {@code expeditionId} and {@code occurredAt} next to the event's own fields, and the stored type and schema
 * version travel beside it. Evolving a payload means a new version and an upcast in {@link #decode}.
 */
public final class EventPayloadCodec {
    public static final int SCHEMA_VERSION = 1;

    public Map<String, Object> encode(DomainEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("expeditionId", event.expeditionId().value().toString());
        payload.put("occurredAt", event.occurredAt().toString());
        switch (event) {
            case ExpeditionApproved approved -> {
                payload.put("version", approved.version());
                approved.superseded().ifPresent(id -> payload.put("superseded", id.value().toString()));
            }
            case RunStarted started -> {
            }
            case ActivityStarted started -> payload.put("activityId", started.activityId().value().toString());
            case ActivityCompleted completed -> {
                payload.put("activityId", completed.activityId().value().toString());
                payload.put("startedAt", completed.startedAt().toString());
                payload.put("result", completed.result().text());
                Map<String, Object> consumed = new LinkedHashMap<>();
                completed.consumed().forEach((id, amount) -> consumed.put(id.value().toString(), amount.amount()));
                payload.put("consumed", consumed);
            }
            case IncidentReported reported -> {
                payload.put("incidentId", reported.incidentId().value().toString());
                reported.activityId().ifPresent(id -> payload.put("activityId", id.value().toString()));
                payload.put("description", reported.description().text());
            }
            case RunFinished finished -> {
            }
            case ReplanProposed proposed -> {
                payload.put("proposalId", proposed.proposalId().value().toString());
                payload.put("suggested", proposed.suggested().value().toString());
                payload.put("incidentId", proposed.incidentId().value().toString());
            }
            case ReplanDecided decided -> {
                payload.put("proposalId", decided.proposalId().value().toString());
                payload.put("decision", decided.decision().name());
                payload.put("decidedBy", decided.decidedBy().value().toString());
            }
        }
        return payload;
    }

    public DomainEvent decode(String type, int schemaVersion, Map<String, Object> payload) {
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalStateException("unsupported schema version " + schemaVersion + " of event " + type);
        }
        ExpeditionId expeditionId = new ExpeditionId(uuid(payload, "expeditionId"));
        Instant occurredAt = instant(payload, "occurredAt");
        return switch (type) {
            case "ExpeditionApproved" -> new ExpeditionApproved(
                    expeditionId,
                    occurredAt,
                    number(payload, "version"),
                    optionalUuid(payload, "superseded").map(ExpeditionId::new)
            );
            case "RunStarted" -> new RunStarted(expeditionId, occurredAt);
            case "ActivityStarted" -> new ActivityStarted(expeditionId, occurredAt, new ActivityId(uuid(payload, "activityId")));
            case "ActivityCompleted" -> new ActivityCompleted(
                    expeditionId,
                    occurredAt,
                    new ActivityId(uuid(payload, "activityId")),
                    instant(payload, "startedAt"),
                    new Outcome(text(payload, "result")),
                    consumed(payload)
            );
            case "IncidentReported" -> new IncidentReported(
                    expeditionId,
                    occurredAt,
                    new IncidentId(uuid(payload, "incidentId")),
                    optionalUuid(payload, "activityId").map(ActivityId::new),
                    new IncidentDescription(text(payload, "description"))
            );
            case "RunFinished" -> new RunFinished(expeditionId, occurredAt);
            case "ReplanProposed" -> new ReplanProposed(
                    expeditionId,
                    occurredAt,
                    new ProposalId(uuid(payload, "proposalId")),
                    new ExpeditionId(uuid(payload, "suggested")),
                    new IncidentId(uuid(payload, "incidentId"))
            );
            case "ReplanDecided" -> new ReplanDecided(
                    expeditionId,
                    occurredAt,
                    new ProposalId(uuid(payload, "proposalId")),
                    ReplanProposal.Decision.valueOf(text(payload, "decision")),
                    new PersonId(uuid(payload, "decidedBy"))
            );
            default -> throw new IllegalStateException("unknown event type: " + type);
        };
    }

    private static Map<ConsumableId, Consumption> consumed(Map<String, Object> payload) {
        Object raw = required(payload, "consumed");
        if (!(raw instanceof Map<?, ?> amounts)) {
            throw new IllegalStateException("consumed is not an object: " + raw);
        }
        Map<ConsumableId, Consumption> consumed = new LinkedHashMap<>();
        amounts.forEach((id, amount) -> {
            if (!(amount instanceof Number quantity)) {
                throw new IllegalStateException("consumed amount is not a number: " + amount);
            }
            consumed.put(new ConsumableId(UUID.fromString(String.valueOf(id))), new Consumption(quantity.intValue()));
        });
        return consumed;
    }

    private static Object required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new IllegalStateException("event payload has no " + key);
        }
        return value;
    }

    private static String text(Map<String, Object> payload, String key) {
        return String.valueOf(required(payload, key));
    }

    private static UUID uuid(Map<String, Object> payload, String key) {
        return UUID.fromString(text(payload, key));
    }

    private static Optional<UUID> optionalUuid(Map<String, Object> payload, String key) {
        return Optional.ofNullable(payload.get(key)).map(String::valueOf).map(UUID::fromString);
    }

    private static Instant instant(Map<String, Object> payload, String key) {
        return Instant.parse(text(payload, key));
    }

    private static int number(Map<String, Object> payload, String key) {
        if (!(required(payload, key) instanceof Number number)) {
            throw new IllegalStateException("event payload " + key + " is not a number");
        }
        return number.intValue();
    }
}
