package edu.itba.fieldops.domain.support;

import edu.itba.fieldops.domain.expedition.Proposals;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.IncidentId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class StoredProposals implements Proposals {
    private final Map<IncidentId, ReplanProposal> proposals = new LinkedHashMap<>();

    public void save(ReplanProposal proposal) {
        proposals.put(proposal.incident().id(), proposal);
    }

    @Override
    public Optional<ReplanProposal> forIncident(IncidentId id) {
        return Optional.ofNullable(proposals.get(id));
    }
}
