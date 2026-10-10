package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.domain.tracking.Observation;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record StoredObservation(
        @Column(name = "note", nullable = false) String note,
        @Column(name = "recorded_at", nullable = false) Instant recordedAt
) {
    static StoredObservation of(Observation observation) {
        return new StoredObservation(observation.text(), observation.at());
    }

    Observation toDomain() {
        return new Observation(note, recordedAt);
    }
}
