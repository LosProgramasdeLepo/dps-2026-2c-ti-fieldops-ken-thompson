package edu.itba.fieldops.adapters.jpa;

import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;
import java.util.function.Function;

public final class JpaPages {
    public static final Sort IN_REGISTRATION_ORDER = Sort.by("registrationOrder");

    private JpaPages() {
    }

    public static Pageable pageable(PageRequest request) {
        Objects.requireNonNull(request, "page request");
        return org.springframework.data.domain.PageRequest.of(request.number(), request.size(), IN_REGISTRATION_ORDER);
    }

    public static Pageable unsorted(PageRequest request) {
        Objects.requireNonNull(request, "page request");
        return org.springframework.data.domain.PageRequest.of(request.number(), request.size());
    }

    public static <E, T> Page<T> page(
            org.springframework.data.domain.Page<E> stored,
            PageRequest request,
            Function<E, T> toDomain
    ) {
        return new Page<>(stored.getContent().stream().map(toDomain).toList(), request, stored.getTotalElements());
    }
}
