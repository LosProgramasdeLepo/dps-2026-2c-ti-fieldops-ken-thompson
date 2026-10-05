package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.InstrumentId;

import java.util.List;
import java.util.Optional;

public interface Instruments {
    Optional<Instrument> instrument(InstrumentId id);

    List<Instrument> instruments();
}
