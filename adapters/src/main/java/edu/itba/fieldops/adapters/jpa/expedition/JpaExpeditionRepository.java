package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.jpa.JpaPages;
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
@Profile(PersistenceProfiles.SQL)
@Transactional(readOnly = true)
public class JpaExpeditionRepository implements ExpeditionRepository {
    private final ExpeditionJpaRepository expeditions;
    private final ExpeditionRegistrationJpaRepository registrations;
    private final StoredExpeditions stored;

    public JpaExpeditionRepository(
            ExpeditionJpaRepository expeditions,
            ExpeditionRegistrationJpaRepository registrations) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        this.stored = new StoredExpeditions(expeditions);
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
        Objects.requireNonNull(expedition, "expedition");
        stored.save(expedition);
        UUID id = expedition.id().value();
        if (!registrations.existsById(id)) {
            registrations.save(new ExpeditionRegistrationEntity(id));
        }
    }

    @Override
    public Optional<Expedition> find(ExpeditionId id) {
        Objects.requireNonNull(id, "expedition id");
        return expeditions.findRegisteredById(id.value()).map(ExpeditionEntity::toDomain);
    }

    @Override
    public List<Expedition> all() {
        return expeditions.findAllRegistered().stream().map(ExpeditionEntity::toDomain).toList();
    }

    @Override
    public Page<Expedition> all(PageRequest request) {
        return JpaPages.page(expeditions.findAllRegistered(JpaPages.unsorted(request)), request, ExpeditionEntity::toDomain);
    }
}
