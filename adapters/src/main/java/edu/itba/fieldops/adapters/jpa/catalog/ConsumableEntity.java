package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "consumables")
public class ConsumableEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int stock;

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected ConsumableEntity() {
    }

    ConsumableEntity(Consumable consumable) {
        this.id = consumable.id().value();
        this.name = consumable.name();
        this.stock = consumable.stock().amount();
    }

    public Consumable toDomain() {
        return new Consumable(new ConsumableId(id), name, new Stock(stock));
    }
}
