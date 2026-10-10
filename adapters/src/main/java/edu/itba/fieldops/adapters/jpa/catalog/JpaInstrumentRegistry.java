package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.usecase.catalog.InstrumentRegistry;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JpaInstrumentRegistry implements InstrumentRegistry {
    private final StoredCatalog<InstrumentEntity, Instrument> stored;

    public JpaInstrumentRegistry(InstrumentJpaRepository repository, TransactionOperations transactions) {
        this.stored = new StoredCatalog<>(repository, InstrumentEntity::toDomain, transactions);
    }

    @Override
    public InstrumentId nextInstrumentId() {
        return new InstrumentId(UUID.randomUUID());
    }

    @Override
    public void save(Instrument instrument) {
        Objects.requireNonNull(instrument, "instrument");
        stored.save(() -> new InstrumentEntity(instrument));
    }

    @Override
    public Optional<Instrument> instrument(InstrumentId id) {
        return stored.find(Objects.requireNonNull(id, "instrument id").value());
    }

    @Override
    public List<Instrument> instruments() {
        return stored.all();
    }

    @Override
    public Page<Instrument> instruments(PageRequest request) {
        return stored.page(request);
    }
}
