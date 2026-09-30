package edu.itba.fieldops.domain.shared;

import java.time.Instant;

public interface Clock {
    Instant now();
}
