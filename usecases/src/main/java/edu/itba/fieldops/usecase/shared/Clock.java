package edu.itba.fieldops.usecase.shared;

import java.time.Instant;

public interface Clock {
    Instant now();
}
