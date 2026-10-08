package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permits")
public class PermitEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String zone;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(nullable = false)
    private String kind;

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected PermitEntity() {
    }

    PermitEntity(Permit permit) {
        this.id = permit.id().value();
        this.zone = permit.zone().name();
        this.validFrom = permit.validity().start();
        this.validTo = permit.validity().end();
        this.kind = permit.kind().name();
    }

    public Permit toDomain() {
        return new Permit(
                new PermitId(id),
                new WorkZone(zone),
                new TimePeriod(validFrom, validTo),
                new PermitKind(kind)
        );
    }
}
