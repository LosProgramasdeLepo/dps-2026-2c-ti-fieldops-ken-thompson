package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class Revisions {
    private Revisions() {
    }

    static Expedition inForce(Expedition plan, List<Expedition> all) {
        Expedition current = plan;
        while (current.status() == ExpeditionStatus.SUPERSEDED) {
            current = approvedRevisionOf(current, all);
        }
        return current;
    }

    static Set<ExpeditionId> lineage(Expedition plan, List<Expedition> all) {
        Set<ExpeditionId> lineage = new HashSet<>();
        lineage.add(plan.id());
        Optional<ExpeditionId> previous = plan.supersedes();
        while (previous.isPresent() && lineage.add(previous.get())) {
            previous = find(all, previous.get()).flatMap(Expedition::supersedes);
        }
        return lineage;
    }

    private static Expedition approvedRevisionOf(Expedition plan, List<Expedition> all) {
        return all.stream()
                .filter(candidate -> candidate.supersedes().filter(plan.id()::equals).isPresent())
                .filter(candidate -> candidate.status() == ExpeditionStatus.APPROVED
                        || candidate.status() == ExpeditionStatus.SUPERSEDED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("superseded plan without an approved revision: " + plan.id()));
    }

    private static Optional<Expedition> find(List<Expedition> all, ExpeditionId id) {
        return all.stream().filter(candidate -> candidate.id().equals(id)).findFirst();
    }
}
