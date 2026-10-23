package edu.itba.fieldops.domain.shared;

public record Consumption(int amount) {
    public Consumption {
        if (amount < 1) {
            throw new InvalidValue("consumption must be positive: " + amount);
        }
    }

    public Consumption plus(Consumption other) {
        return new Consumption(Math.addExact(amount, other.amount));
    }
}
