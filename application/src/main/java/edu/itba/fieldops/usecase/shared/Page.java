package edu.itba.fieldops.usecase.shared;

import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record Page<T>(List<T> items, PageRequest request, long totalItems) {
    public Page {
        items = List.copyOf(items);
        Objects.requireNonNull(request, "page request");
        if (items.size() > request.size()) {
            throw new InvalidValue("page holds more items than its size: " + items.size());
        }
        if (totalItems < items.size()) {
            throw new InvalidValue("total items must cover the page: " + totalItems);
        }
    }

    public int totalPages() {
        return Math.toIntExact((totalItems + request.size() - 1) / request.size());
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        return new Page<>(items.stream().<R>map(mapper).toList(), request, totalItems);
    }
}
