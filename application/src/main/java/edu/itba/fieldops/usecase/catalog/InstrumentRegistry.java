package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Instruments;
import edu.itba.fieldops.domain.identity.InstrumentId;

public interface InstrumentRegistry extends Instruments {
    InstrumentId nextInstrumentId();

    void save(Instrument instrument);
}
