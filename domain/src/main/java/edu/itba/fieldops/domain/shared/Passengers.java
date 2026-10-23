package edu.itba.fieldops.domain.shared;

public record Passengers(int count) {
    public Passengers {
        if (count < 0) {
            throw new InvalidValue("passengers must not be negative: " + count);
        }
    }
}
