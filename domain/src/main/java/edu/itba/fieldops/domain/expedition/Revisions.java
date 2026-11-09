package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.UnknownResource;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Revisions {
    private Revisions() {
    }

    public static Expedition inForce(Expedition plan, List<Expedition> all) {
        Expedition current = plan;
        while (current.status() == ExpeditionStatus.SUPERSEDED) {
            current = approvedRevisionOf(current, all);
        }
        return current;
    }

    public static Set<ExpeditionId> lineage(Expedition plan, List<Expedition> all) {
        Set<ExpeditionId> lineage = new HashSet<>();
        lineage.add(plan.id());
        Optional<ExpeditionId> previous = plan.supersedes();
        while (previous.isPresent() && lineage.add(previous.get())) {
            previous = find(all, previous.get()).flatMap(Expedition::supersedes);
        }
        return lineage;
    }

    public static Set<ExpeditionId> family(Expedition plan, List<Expedition> all) {
        Set<ExpeditionId> family = new LinkedHashSet<>(lineage(plan, all));
        boolean grew = true;
        while (grew) {
            grew = all.stream()
                    .filter(candidate -> candidate.supersedes().filter(family::contains).isPresent())
                    .map(Expedition::id)
                    .map(family::add)
                    .reduce(false, Boolean::logicalOr);
        }
        return family;
    }

    private static Expedition approvedRevisionOf(Expedition plan, List<Expedition> all) {
        return all.stream()
                .filter(candidate -> candidate.supersedes().filter(plan.id()::equals).isPresent())
                .filter(candidate -> candidate.status() == ExpeditionStatus.APPROVED
                        || candidate.status() == ExpeditionStatus.SUPERSEDED)
                .findFirst()
                .orElseThrow(() -> new UnknownResource("approved revision of expedition", plan.id().value().toString()));
    }

    private static Optional<Expedition> find(List<Expedition> all, ExpeditionId id) {
        return all.stream().filter(candidate -> candidate.id().equals(id)).findFirst();
    }
}
