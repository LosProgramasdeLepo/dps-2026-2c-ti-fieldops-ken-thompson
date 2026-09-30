package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.List;

public final class ExpeditionEditing {
    private ExpeditionEditing() {
    }

    public static Expedition draft(
            ExpeditionId id,
            List<Objective> objectives,
            TimePeriod period,
            List<WorkZone> zones,
            List<PersonId> responsibles,
            List<Restriction> restrictions
    ) {
        return Expedition.draft(id, objectives, period, zones, responsibles, restrictions);
    }

    public static void addActivity(Expedition expedition, Activity activity) {
        expedition.addActivity(activity);
    }

    public static void addBlock(Expedition expedition, ActivityBlock block) {
        expedition.addBlock(block);
    }

    public static void addAssignment(Expedition expedition, Assignment assignment) {
        expedition.addAssignment(assignment);
    }

    public static void addPermit(Expedition expedition, PermitId permitId) {
        expedition.addPermit(permitId);
    }

    public static void acceptWarning(Expedition expedition, AcceptedWarning warning) {
        expedition.acceptWarning(warning);
    }

    public static void submitForReview(Expedition expedition) {
        expedition.submitForReview();
    }
}
