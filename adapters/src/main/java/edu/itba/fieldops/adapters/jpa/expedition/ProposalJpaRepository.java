package edu.itba.fieldops.adapters.jpa.expedition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProposalJpaRepository extends JpaRepository<ProposalEntity, UUID> {
    Page<ProposalEntity> findByOriginalId(UUID originalId, Pageable pageable);

    Optional<ProposalEntity> findByIncidentId(UUID incidentId);
}
