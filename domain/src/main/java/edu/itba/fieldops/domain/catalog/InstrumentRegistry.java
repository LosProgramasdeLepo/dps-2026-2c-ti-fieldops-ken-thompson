package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.InstrumentId;

public interface InstrumentRegistry extends Instruments {
    InstrumentId nextInstrumentId();

    void save(Instrument instrument);
}
