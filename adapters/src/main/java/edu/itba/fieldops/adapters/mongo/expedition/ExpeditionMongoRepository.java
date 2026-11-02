package edu.itba.fieldops.adapters.mongo.expedition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

interface ExpeditionMongoRepository extends MongoRepository<ExpeditionDocument, String> {
    Optional<ExpeditionDocument> findByIdAndRegistrationOrderNotNull(String id);

    List<ExpeditionDocument> findByRegistrationOrderNotNullOrderByRegistrationOrder();

    Page<ExpeditionDocument> findByRegistrationOrderNotNull(Pageable pageable);
}
