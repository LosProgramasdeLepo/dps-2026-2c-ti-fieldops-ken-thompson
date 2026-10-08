package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.adapters.jpa.JpaPages;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

final class StoredCatalog<E, T> {
    private final JpaRepository<E, UUID> repository;
    private final Function<E, T> toDomain;
    private final TransactionOperations transactions;

    StoredCatalog(JpaRepository<E, UUID> repository, Function<E, T> toDomain, TransactionOperations transactions) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.toDomain = Objects.requireNonNull(toDomain, "to domain");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    void save(Supplier<E> entity) {
        transactions.executeWithoutResult(status -> repository.save(entity.get()));
    }

    Optional<T> find(UUID id) {
        Objects.requireNonNull(id, "id");
        return transactions.execute(status -> repository.findById(id).map(toDomain));
    }

    List<T> all() {
        return transactions.execute(status -> repository.findAll(JpaPages.IN_REGISTRATION_ORDER).stream()
                .map(toDomain)
                .toList());
    }

    Page<T> page(PageRequest request) {
        return transactions.execute(status -> JpaPages.page(repository.findAll(JpaPages.pageable(request)), request, toDomain));
    }
}
