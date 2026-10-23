package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.catalog.CertificationRegistry;
import edu.itba.fieldops.domain.catalog.ConsumableRegistry;
import edu.itba.fieldops.domain.catalog.InstrumentRegistry;
import edu.itba.fieldops.domain.catalog.PermitRegistry;
import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.catalog.VehicleRegistry;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;

import java.util.Objects;

public record FieldOpsRepositories(
        CertificationRegistry certifications,
        PersonRegistry people,
        VehicleRegistry vehicles,
        InstrumentRegistry instruments,
        ConsumableRegistry consumables,
        PermitRegistry permits,
        ExpeditionRepository plans,
        ExecutionRepository runs,
        ReplanProposalRepository proposals
) {
    public FieldOpsRepositories {
        Objects.requireNonNull(certifications, "certifications");
        Objects.requireNonNull(people, "people");
        Objects.requireNonNull(vehicles, "vehicles");
        Objects.requireNonNull(instruments, "instruments");
        Objects.requireNonNull(consumables, "consumables");
        Objects.requireNonNull(permits, "permits");
        Objects.requireNonNull(plans, "plans");
        Objects.requireNonNull(runs, "runs");
        Objects.requireNonNull(proposals, "proposals");
    }

    public Catalogs catalogs() {
        return new Catalogs(new BookableResources(people, vehicles, instruments), consumables, permits);
    }
}
