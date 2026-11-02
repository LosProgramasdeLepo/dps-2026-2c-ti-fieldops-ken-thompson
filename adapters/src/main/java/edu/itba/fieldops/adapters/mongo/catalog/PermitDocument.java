package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.mongo.PeriodDocument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = PermitDocument.COLLECTION)
record PermitDocument(@Id String id, String kind, String zone, PeriodDocument validity, long registrationOrder)
        implements RegisteredDocument {
    static final String COLLECTION = "permits";

    static PermitDocument of(Permit permit, long registrationOrder) {
        return new PermitDocument(
                permit.id().value().toString(),
                permit.kind().name(),
                permit.zone().name(),
                PeriodDocument.of(permit.validity()),
                registrationOrder
        );
    }

    Permit toDomain() {
        return new Permit(new PermitId(UUID.fromString(id)), new WorkZone(zone), validity.toDomain(), new PermitKind(kind));
    }
}
