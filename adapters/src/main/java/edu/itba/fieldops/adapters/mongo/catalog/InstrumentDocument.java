package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.mongo.PeriodDocument;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(collection = InstrumentDocument.COLLECTION)
record InstrumentDocument(@Id String id, String kind, List<PeriodDocument> availability, long registrationOrder)
        implements RegisteredDocument {
    static final String COLLECTION = "instruments";

    static InstrumentDocument of(Instrument instrument, long registrationOrder) {
        return new InstrumentDocument(
                instrument.id().value().toString(),
                instrument.kind().name(),
                PeriodDocument.of(instrument.availability()),
                registrationOrder
        );
    }

    Instrument toDomain() {
        return new Instrument(new InstrumentId(UUID.fromString(id)), new InstrumentKind(kind), PeriodDocument.availability(availability));
    }
}
