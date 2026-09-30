package edu.itba.fieldops.domain.expedition;

import java.util.List;
import java.util.Optional;

final class Revisions {
    private Revisions() {
    }

    static Expedition inForce(Expedition plan, List<Expedition> all) {
        Expedition current = plan;
        Optional<Expedition> successor = successorOf(current, all);
        while (successor.isPresent()) {
            current = successor.get();
            successor = successorOf(current, all);
        }
        return current;
    }

    private static Optional<Expedition> successorOf(Expedition plan, List<Expedition> all) {
        return all.stream()
                .filter(candidate -> candidate.supersedes().filter(plan.id()::equals).isPresent())
                .findFirst();
    }
}
