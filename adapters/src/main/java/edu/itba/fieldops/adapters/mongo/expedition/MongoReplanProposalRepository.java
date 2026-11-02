package edu.itba.fieldops.adapters.mongo.expedition;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.MongoPages;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoReplanProposalRepository implements ReplanProposalRepository {
    private final ProposalMongoRepository proposals;
    private final StoredPlans stored;
    private final RegistrationSequence sequence;

    MongoReplanProposalRepository(
            ProposalMongoRepository proposals,
            ExpeditionMongoRepository expeditions,
            RegistrationSequence sequence
    ) {
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.stored = new StoredPlans(expeditions, sequence);
        this.sequence = Objects.requireNonNull(sequence, "sequence");
    }

    @Override
    public ProposalId nextId() {
        return new ProposalId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(ReplanProposal proposal) {
        Objects.requireNonNull(proposal, "proposal");
        stored.keep(proposal.suggested());
        long order = proposals.findById(proposal.id().value().toString())
                .map(ProposalDocument::registrationOrder)
                .orElseGet(() -> sequence.next(ProposalDocument.COLLECTION));
        proposals.save(ProposalDocument.of(proposal.state(), order));
    }

    @Override
    public Optional<ReplanProposal> find(ProposalId id) {
        return proposals.findById(Objects.requireNonNull(id, "proposal id").value().toString()).map(this::toDomain);
    }

    @Override
    public Page<ReplanProposal> of(ExpeditionId originalId, PageRequest request) {
        Objects.requireNonNull(originalId, "expedition id");
        return MongoPages.page(
                proposals.findByOriginalId(originalId.value().toString(), MongoPages.pageable(request)), request, this::toDomain
        );
    }

    private ReplanProposal toDomain(ProposalDocument document) {
        return document.toDomain(stored.find(document.suggestedId()).orElseThrow());
    }
}
