package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.DomainException;

public final class InvalidItinerary extends DomainException {
    public InvalidItinerary(String message) {
        super(message);
    }
}
