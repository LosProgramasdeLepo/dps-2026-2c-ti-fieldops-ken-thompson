package edu.itba.fieldops.domain.identity;

public sealed interface BookableId permits PersonId, VehicleId, InstrumentId {
    String label();
}
