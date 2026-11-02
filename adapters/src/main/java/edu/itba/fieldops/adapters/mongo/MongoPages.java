package edu.itba.fieldops.adapters.mongo;

import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

public final class MongoPages {
    public static final Sort IN_REGISTRATION_ORDER = Sort.by("registrationOrder");

    private MongoPages() {
    }

    public static Pageable pageable(PageRequest request) {
        return org.springframework.data.domain.PageRequest.of(request.number(), request.size(), IN_REGISTRATION_ORDER);
    }

    public static <D, T> Page<T> page(org.springframework.data.domain.Page<D> stored, PageRequest request, Function<D, T> toDomain) {
        return new Page<>(stored.getContent().stream().map(toDomain).toList(), request, stored.getTotalElements());
    }
}
