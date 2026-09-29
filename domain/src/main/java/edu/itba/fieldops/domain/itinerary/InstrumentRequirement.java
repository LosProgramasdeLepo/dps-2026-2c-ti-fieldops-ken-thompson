package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.InstrumentKind;

import java.util.Objects;
import java.util.Optional;

public sealed interface InstrumentRequirement permits InstrumentRequirement.None, InstrumentRequirement.OfKind {
    Optional<InstrumentKind> requiredKind();

    record None() implements InstrumentRequirement {
        @Override
        public Optional<InstrumentKind> requiredKind() {
            return Optional.empty();
        }
    }

    record OfKind(InstrumentKind kind) implements InstrumentRequirement {
        public OfKind {
            Objects.requireNonNull(kind, "instrument kind");
        }

        @Override
        public Optional<InstrumentKind> requiredKind() {
            return Optional.of(kind);
        }
    }
}
