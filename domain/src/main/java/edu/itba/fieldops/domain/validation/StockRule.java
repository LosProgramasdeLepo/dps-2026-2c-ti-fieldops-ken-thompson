package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Consumables;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StockRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(PlanningContext context) {
        Consumables consumables = context.consumables();
        Map<ConsumableId, Stock> needed = new HashMap<>();
        add(needed, context.plan());
        for (Expedition occupying : context.occupying().plans()) {
            add(needed, occupying);
        }
        List<ValidationIssue> issues = new ArrayList<>();
        for (Map.Entry<ConsumableId, Stock> entry : needed.entrySet()) {
            Optional<Consumable> consumable = consumables.consumable(entry.getKey());
            if (consumable.isPresent() && !consumable.get().hasAtLeast(entry.getValue())) {
                issues.add(stockIssue(consumable.get(), entry.getValue()));
            }
        }
        return issues;
    }

    private static void add(Map<ConsumableId, Stock> needed, Expedition expedition) {
        expedition.assignments().consumption().forEach((id, quantity) -> needed.merge(id, quantity, Stock::plus));
    }

    private static ValidationIssue stockIssue(Consumable consumable, Stock needed) {
        return new ValidationIssue(
                IssueSeverity.CRITICAL,
                "STOCK",
                "consumable " + consumable.name()
                        + " stock " + consumable.stock().amount()
                        + " is less than assigned " + needed.amount()
        );
    }
}
