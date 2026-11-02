package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.InstrumentRegistry;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.MEMORY)
public class InMemoryInstrumentRegistry implements InstrumentRegistry {
    private final Map<InstrumentId, Instrument> instruments = new LinkedHashMap<>();

    @Override
    public InstrumentId nextInstrumentId() {
        return new InstrumentId(UUID.randomUUID());
    }

    @Override
    public synchronized void save(Instrument instrument) {
        Objects.requireNonNull(instrument, "instrument");
        instruments.put(instrument.id(), instrument);
    }

    @Override
    public synchronized Optional<Instrument> instrument(InstrumentId id) {
        Objects.requireNonNull(id, "instrument id");
        return Optional.ofNullable(instruments.get(id));
    }

    @Override
    public synchronized List<Instrument> instruments() {
        return List.copyOf(instruments.values());
    }

    @Override
    public synchronized Page<Instrument> instruments(PageRequest request) {
        return InMemoryPages.slice(instruments.values(), request);
    }
}
