package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.InstrumentRegistry;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoInstrumentRegistry implements InstrumentRegistry {
    private final StoredCatalog<InstrumentDocument, Instrument> stored;

    MongoInstrumentRegistry(InstrumentMongoRepository repository, RegistrationSequence sequence) {
        this.stored = new StoredCatalog<>(repository, sequence, InstrumentDocument.COLLECTION, InstrumentDocument::toDomain);
    }

    @Override
    public InstrumentId nextInstrumentId() {
        return new InstrumentId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Instrument instrument) {
        Objects.requireNonNull(instrument, "instrument");
        stored.save(instrument.id().value(), order -> InstrumentDocument.of(instrument, order));
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
