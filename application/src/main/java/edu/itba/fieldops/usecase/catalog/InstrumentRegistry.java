package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Instruments;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Objects;

public interface InstrumentRegistry extends Instruments {
    InstrumentId nextInstrumentId();

    void save(Instrument instrument);

    Page<Instrument> instruments(PageRequest request);

    default Instrument require(InstrumentId id) {
        return instrument(Objects.requireNonNull(id, "instrument id"))
                .orElseThrow(() -> new UnknownResource("instrument", id.value().toString()));
    }
}
