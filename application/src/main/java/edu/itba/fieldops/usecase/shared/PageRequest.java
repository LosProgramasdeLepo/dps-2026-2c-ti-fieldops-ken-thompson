package edu.itba.fieldops.usecase.shared;

import edu.itba.fieldops.domain.shared.InvalidValue;

public record PageRequest(int number, int size) {
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (number < 0) {
            throw new InvalidValue("page number must not be negative: " + number);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidValue("page size must be between 1 and " + MAX_SIZE + ": " + size);
        }
    }

    public long offset() {
        return (long) number * size;
    }
}
