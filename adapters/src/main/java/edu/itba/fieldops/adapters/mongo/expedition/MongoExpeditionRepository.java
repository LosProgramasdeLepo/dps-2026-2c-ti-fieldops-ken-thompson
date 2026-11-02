package edu.itba.fieldops.adapters.mongo.expedition;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.MongoPages;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
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
public class MongoExpeditionRepository implements ExpeditionRepository {
    private final ExpeditionMongoRepository expeditions;
    private final StoredExpeditions stored;

    MongoExpeditionRepository(ExpeditionMongoRepository expeditions, RegistrationSequence sequence) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions");
        this.stored = new StoredExpeditions(expeditions, sequence);
    }

    @Override
    public ExpeditionId nextId() {
        return new ExpeditionId(UUID.randomUUID());
    }

    @Override
    public ActivityId nextActivityId() {
        return new ActivityId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Expedition expedition) {
        stored.register(Objects.requireNonNull(expedition, "expedition"));
    }

    @Override
    public Optional<Expedition> find(ExpeditionId id) {
        return expeditions.findByIdAndRegistrationOrderNotNull(Objects.requireNonNull(id, "expedition id").value().toString())
                .map(ExpeditionDocument::toDomain);
    }

    @Override
    public List<Expedition> all() {
        return expeditions.findByRegistrationOrderNotNullOrderByRegistrationOrder().stream()
                .map(ExpeditionDocument::toDomain)
                .toList();
    }

    @Override
    public Page<Expedition> all(PageRequest request) {
        return MongoPages.page(
                expeditions.findByRegistrationOrderNotNull(MongoPages.pageable(request)), request, ExpeditionDocument::toDomain
        );
    }
}
