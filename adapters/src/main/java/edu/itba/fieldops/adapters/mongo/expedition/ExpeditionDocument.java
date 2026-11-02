package edu.itba.fieldops.adapters.mongo.expedition;

import edu.itba.fieldops.adapters.mongo.PeriodDocument;
import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionState;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Justification;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Document(collection = ExpeditionDocument.COLLECTION)
record ExpeditionDocument(
        @Id String id,
        int version,
        String supersedes,
        String status,
        Charter charter,
        List<ItineraryNode> itinerary,
        AssignmentEntries assignments,
        List<String> permits,
        List<Warning> acceptedWarnings,
        Long registrationOrder
) {
    static final String COLLECTION = "expeditions";

    static ExpeditionDocument of(Expedition expedition, Long registrationOrder) {
        ExpeditionState state = expedition.state();
        return new ExpeditionDocument(
                state.id().value().toString(),
                state.version(),
                state.supersedes().map(id -> id.value().toString()).orElse(null),
                state.status().name(),
                Charter.of(state.charter()),
                state.items().stream().map(ItineraryNode::of).toList(),
                AssignmentEntries.of(state.assignments()),
                state.permits().stream().map(id -> id.value().toString()).toList(),
                state.acceptedWarnings().stream().map(Warning::of).toList(),
                registrationOrder
        );
    }

    Expedition toDomain() {
        return Expedition.restore(new ExpeditionState(
                new ExpeditionId(UUID.fromString(id)),
                version,
                Optional.ofNullable(supersedes).map(UUID::fromString).map(ExpeditionId::new),
                ExpeditionStatus.valueOf(status),
                charter.toDomain(),
                itinerary.stream().map(ItineraryNode::toDomain).toList(),
                assignments.toDomain(),
                permits.stream().map(UUID::fromString).map(PermitId::new).toList(),
                acceptedWarnings.stream().map(Warning::toDomain).toList()
        ));
    }

    record Charter(
            List<String> objectives,
            PeriodDocument period,
            List<String> zones,
            List<String> responsibles,
            List<String> restrictions
    ) {
        static Charter of(ExpeditionCharter charter) {
            return new Charter(
                    charter.objectives().stream().map(Objective::text).toList(),
                    PeriodDocument.of(charter.period()),
                    charter.zones().stream().map(WorkZone::name).toList(),
                    charter.responsibles().stream().map(id -> id.value().toString()).toList(),
                    charter.restrictions().stream().map(Restriction::text).toList()
            );
        }

        ExpeditionCharter toDomain() {
            return new ExpeditionCharter(
                    objectives.stream().map(Objective::new).toList(),
                    period.toDomain(),
                    zones.stream().map(WorkZone::new).toList(),
                    responsibles.stream().map(UUID::fromString).map(PersonId::new).toList(),
                    restrictions.stream().map(Restriction::new).toList()
            );
        }
    }

    record Warning(String severity, String code, String message, String justification, String acceptedBy) {
        static Warning of(AcceptedWarning warning) {
            ValidationIssue issue = warning.issue();
            return new Warning(
                    issue.severity().name(),
                    issue.code(),
                    issue.message(),
                    warning.justification().text(),
                    warning.acceptedBy().value().toString()
            );
        }

        AcceptedWarning toDomain() {
            return new AcceptedWarning(
                    new ValidationIssue(IssueSeverity.valueOf(severity), code, message),
                    new Justification(justification),
                    new PersonId(UUID.fromString(acceptedBy))
            );
        }
    }
}
