package edu.itba.fieldops.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record ProposalId(UUID value) {
    public ProposalId {
        Objects.requireNonNull(value, "proposal id");
    }
}
