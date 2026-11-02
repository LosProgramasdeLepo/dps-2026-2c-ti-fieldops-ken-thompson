package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.ConsumableName;
import edu.itba.fieldops.domain.catalog.Stock;
import edu.itba.fieldops.domain.identity.ConsumableId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = ConsumableDocument.COLLECTION)
record ConsumableDocument(@Id String id, String name, int stock, long registrationOrder) implements RegisteredDocument {
    static final String COLLECTION = "consumables";

    static ConsumableDocument of(Consumable consumable, long registrationOrder) {
        return new ConsumableDocument(
                consumable.id().value().toString(),
                consumable.name().value(),
                consumable.stock().amount(),
                registrationOrder
        );
    }

    Consumable toDomain() {
        return new Consumable(new ConsumableId(UUID.fromString(id)), new ConsumableName(name), new Stock(stock));
    }
}
