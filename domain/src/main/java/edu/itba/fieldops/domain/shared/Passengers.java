package edu.itba.fieldops.domain.shared;

public record Passengers(int count) {
    public static final Passengers ZERO = new Passengers(0);

    public Passengers {
        if (count < 0) {
            throw new InvalidValue("passengers must not be negative: " + count);
        }
    }

    public Passengers plus(Passengers other) {
        return new Passengers(count + other.count);
    }

    public boolean isAtLeast(Passengers other) {
        return count >= other.count;
    }
}
