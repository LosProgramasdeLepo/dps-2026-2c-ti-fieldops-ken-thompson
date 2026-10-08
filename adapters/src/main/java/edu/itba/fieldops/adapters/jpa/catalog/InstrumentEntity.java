package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.jpa.StoredPeriod;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "instruments")
public class InstrumentEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String kind;

    @ElementCollection
    @CollectionTable(name = "instrument_availability", joinColumns = @JoinColumn(name = "instrument_id"))
    @OrderColumn(name = "position")
    private List<StoredPeriod> availability = new ArrayList<>();

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected InstrumentEntity() {
    }

    InstrumentEntity(Instrument instrument) {
        this.id = instrument.id().value();
        this.kind = instrument.kind().name();
        this.availability = new ArrayList<>(StoredPeriod.of(instrument.availability()));
    }

    public Instrument toDomain() {
        return new Instrument(new InstrumentId(id), new InstrumentKind(kind), StoredPeriod.availability(availability));
    }
}
