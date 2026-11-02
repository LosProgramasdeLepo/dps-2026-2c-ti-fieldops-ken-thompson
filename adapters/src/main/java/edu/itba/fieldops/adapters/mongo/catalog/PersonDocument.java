package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.mongo.PeriodDocument;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(collection = PersonDocument.COLLECTION)
record PersonDocument(
        @Id String id,
        String name,
        List<HeldCertification> certifications,
        List<PeriodDocument> availability,
        long registrationOrder
) implements RegisteredDocument {
    static final String COLLECTION = "people";

    static PersonDocument of(Person person, long registrationOrder) {
        return new PersonDocument(
                person.id().value().toString(),
                person.name().value(),
                person.certifications().stream().map(HeldCertification::of).toList(),
                PeriodDocument.of(person.availability()),
                registrationOrder
        );
    }

    Person toDomain() {
        return new Person(
                new PersonId(UUID.fromString(id)),
                new PersonName(name),
                certifications.stream().map(HeldCertification::toDomain).toList(),
                PeriodDocument.availability(availability)
        );
    }

    record HeldCertification(String id, String name) {
        static HeldCertification of(Certification certification) {
            return new HeldCertification(certification.id().value().toString(), certification.name().value());
        }

        Certification toDomain() {
            return new Certification(new CertificationId(UUID.fromString(id)), new CertificationName(name));
        }
    }
}
