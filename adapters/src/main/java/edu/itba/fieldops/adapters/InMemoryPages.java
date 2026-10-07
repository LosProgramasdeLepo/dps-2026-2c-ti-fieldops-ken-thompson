package edu.itba.fieldops.adapters;

import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;

import java.util.Collection;
import java.util.Objects;

final class InMemoryPages {
    private InMemoryPages() {
    }

    static <T> Page<T> slice(Collection<T> items, PageRequest request) {
        Objects.requireNonNull(request, "page request");
        return new Page<>(items.stream().skip(request.offset()).limit(request.size()).toList(), request, items.size());
    }
}
