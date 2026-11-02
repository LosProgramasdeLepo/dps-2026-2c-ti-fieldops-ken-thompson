package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.identity.CertificationId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = CertificationDocument.COLLECTION)
record CertificationDocument(@Id String id, String name, long registrationOrder) implements RegisteredDocument {
    static final String COLLECTION = "certifications";

    static CertificationDocument of(Certification certification, long registrationOrder) {
        return new CertificationDocument(certification.id().value().toString(), certification.name().value(), registrationOrder);
    }

    Certification toDomain() {
        return new Certification(new CertificationId(UUID.fromString(id)), new CertificationName(name));
    }
}
