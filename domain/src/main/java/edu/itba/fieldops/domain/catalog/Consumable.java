package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.Texts;

import java.util.Objects;

public final class Consumable {
    private final ConsumableId id;
    private final String name;
    private final Stock stock;

    public Consumable(ConsumableId id, String name, Stock stock) {
        this.id = Objects.requireNonNull(id, "consumable id");
        this.name = Texts.required(name, "consumable name");
        this.stock = Objects.requireNonNull(stock, "stock");
    }

    public ConsumableId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Stock stock() {
        return stock;
    }

    public Consumable withStock(Stock stock) {
        return new Consumable(id, name, stock);
    }

    public boolean hasAtLeast(Stock needed) {
        return stock.isAtLeast(needed);
    }
}
