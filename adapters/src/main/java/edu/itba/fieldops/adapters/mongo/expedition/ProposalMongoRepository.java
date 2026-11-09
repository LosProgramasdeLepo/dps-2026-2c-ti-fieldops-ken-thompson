package edu.itba.fieldops.adapters.mongo.expedition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

interface ProposalMongoRepository extends MongoRepository<ProposalDocument, String> {
    Page<ProposalDocument> findByOriginalId(String originalId, Pageable pageable);

    Optional<ProposalDocument> findByIncidentId(String incidentId);
}
