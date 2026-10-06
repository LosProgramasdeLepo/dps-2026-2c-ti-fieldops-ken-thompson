package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Texts;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Person implements Bookable {
    private final PersonId id;
    private final String name;
    private final List<Certification> certifications;
    private final Availability availability;

    public Person(PersonId id, String name, List<Certification> certifications, Availability availability) {
        this.id = Objects.requireNonNull(id, "person id");
        this.name = Texts.required(name, "person name");
        this.certifications = List.copyOf(certifications);
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public PersonId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<Certification> certifications() {
        return certifications;
    }

    public Availability availability() {
        return availability;
    }

    public Person withAvailability(Availability availability) {
        return new Person(id, name, certifications, availability);
    }

    public Person certified(Certification certification) {
        Objects.requireNonNull(certification, "certification");
        if (holds(certification.id())) {
            throw new InvalidValue("person already holds certification: " + certification.id());
        }
        List<Certification> next = new ArrayList<>(certifications);
        next.add(certification);
        return new Person(id, name, next, availability);
    }

    public boolean holds(CertificationId certificationId) {
        Objects.requireNonNull(certificationId, "certification id");
        return certifications.stream().anyMatch(held -> held.id().equals(certificationId));
    }

    @Override
    public boolean availableDuring(TimePeriod period) {
        return availability.covers(period);
    }
}
