package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.identity.CertificationId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "certifications")
public class CertificationEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected CertificationEntity() {
    }

    CertificationEntity(Certification certification) {
        this.id = certification.id().value();
        this.name = certification.name();
    }

    public Certification toDomain() {
        return new Certification(new CertificationId(id), name);
    }
}
