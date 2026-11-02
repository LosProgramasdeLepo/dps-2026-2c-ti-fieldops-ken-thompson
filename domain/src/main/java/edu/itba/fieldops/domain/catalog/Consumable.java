package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Consumption;

import java.util.Objects;

public final class Consumable {
    private final ConsumableId id;
    private final ConsumableName name;
    private final Stock stock;

    public Consumable(ConsumableId id, ConsumableName name, Stock stock) {
        this.id = Objects.requireNonNull(id, "consumable id");
        this.name = Objects.requireNonNull(name, "consumable name");
        this.stock = Objects.requireNonNull(stock, "stock");
    }

    public ConsumableId id() {
        return id;
    }

    public ConsumableName name() {
        return name;
    }

    public Stock stock() {
        return stock;
    }

    public Consumable withStock(Stock stock) {
        return new Consumable(id, name, stock);
    }

    public boolean covers(Consumption needed) {
        return stock.covers(needed);
    }
}
