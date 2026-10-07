package edu.itba.fieldops.api.json;

import edu.itba.fieldops.usecase.shared.PageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Objects;

public record PageQuery(@Min(0) Integer page, @Min(1) @Max(PageRequest.MAX_SIZE) Integer size) {
    private static final int DEFAULT_SIZE = 20;

    public PageQuery {
        page = Objects.requireNonNullElse(page, 0);
        size = Objects.requireNonNullElse(size, DEFAULT_SIZE);
    }
}
