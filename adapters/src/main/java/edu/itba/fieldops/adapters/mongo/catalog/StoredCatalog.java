package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.mongo.MongoPages;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongFunction;

final class StoredCatalog<D extends RegisteredDocument, T> {
    private final MongoRepository<D, String> repository;
    private final RegistrationSequence sequence;
    private final String collection;
    private final Function<D, T> toDomain;

    StoredCatalog(MongoRepository<D, String> repository, RegistrationSequence sequence, String collection, Function<D, T> toDomain) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.sequence = Objects.requireNonNull(sequence, "sequence");
        this.collection = Objects.requireNonNull(collection, "collection");
        this.toDomain = Objects.requireNonNull(toDomain, "to domain");
    }

    void save(UUID id, LongFunction<D> document) {
        long order = repository.findById(id.toString())
                .map(RegisteredDocument::registrationOrder)
                .orElseGet(() -> sequence.next(collection));
        repository.save(document.apply(order));
    }

    Optional<T> find(UUID id) {
        return repository.findById(Objects.requireNonNull(id, "id").toString()).map(toDomain);
    }

    List<T> all() {
        return repository.findAll(MongoPages.IN_REGISTRATION_ORDER).stream().map(toDomain).toList();
    }

    Page<T> page(PageRequest request) {
        return MongoPages.page(repository.findAll(MongoPages.pageable(request)), request, toDomain);
    }
}
