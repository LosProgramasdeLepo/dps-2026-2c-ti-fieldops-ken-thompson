package edu.itba.fieldops.domain.shared;

public record Stock(int amount) {
    public Stock {
        if (amount < 0) {
            throw new InvalidValue("stock must not be negative: " + amount);
        }
    }

    public Stock plus(Stock other) {
        return new Stock(amount + other.amount);
    }

    public boolean isAtLeast(Stock other) {
        return amount >= other.amount;
    }
}
