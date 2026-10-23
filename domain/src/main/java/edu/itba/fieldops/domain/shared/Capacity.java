package edu.itba.fieldops.domain.shared;

public record Capacity(int seats) {
    public Capacity {
        if (seats < 1) {
            throw new InvalidValue("capacity must be positive: " + seats);
        }
    }

    public Capacity plus(Capacity other) {
        return new Capacity(Math.addExact(seats, other.seats));
    }

    public boolean carries(Passengers passengers) {
        return seats >= passengers.count();
    }
}
