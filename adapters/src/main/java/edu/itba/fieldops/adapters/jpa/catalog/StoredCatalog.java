package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.jpa.JpaPages;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

final class StoredCatalog<E, T> {
    private final JpaRepository<E, UUID> repository;
    private final Function<E, T> toDomain;

    StoredCatalog(JpaRepository<E, UUID> repository, Function<E, T> toDomain) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.toDomain = Objects.requireNonNull(toDomain, "to domain");
    }

    void save(E entity) {
        repository.save(entity);
    }

    Optional<T> find(UUID id) {
        return repository.findById(Objects.requireNonNull(id, "id")).map(toDomain);
    }

    List<T> all() {
        return repository.findAll(JpaPages.IN_REGISTRATION_ORDER).stream().map(toDomain).toList();
    }

    Page<T> page(PageRequest request) {
        return JpaPages.page(repository.findAll(JpaPages.pageable(request)), request, toDomain);
    }
}
