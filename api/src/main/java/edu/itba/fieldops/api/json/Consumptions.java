package edu.itba.fieldops.api.json;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Consumption;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Consumptions {
    private Consumptions() {
    }

    public static Map<ConsumableId, Consumption> toConsumption(Map<UUID, Integer> amounts) {
        Map<ConsumableId, Consumption> consumption = new LinkedHashMap<>();
        amounts.forEach((id, amount) -> consumption.put(new ConsumableId(id), new Consumption(amount)));
        return consumption;
    }

    public static Map<String, Integer> toResponse(Map<ConsumableId, Consumption> consumption) {
        Map<String, Integer> amounts = new LinkedHashMap<>();
        consumption.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ConsumableId::value)))
                .forEach(entry -> amounts.put(entry.getKey().value().toString(), entry.getValue().amount()));
        return amounts;
    }
}
