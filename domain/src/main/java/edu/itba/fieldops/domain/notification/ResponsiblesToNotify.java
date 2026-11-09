package edu.itba.fieldops.domain.notification;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.expedition.Plans;
import edu.itba.fieldops.domain.expedition.Revisions;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ResponsiblesToNotify {
    private static final Set<Class<? extends DomainEvent>> CONCERNING = Set.of(
            ExpeditionApproved.class,
            IncidentReported.class,
            ReplanProposed.class
    );

    private final Plans plans;

    public ResponsiblesToNotify(Plans plans) {
        this.plans = Objects.requireNonNull(plans, "plans");
    }

    public boolean concern(DomainEvent event) {
        return CONCERNING.contains(Objects.requireNonNull(event, "event").getClass());
    }

    public List<Notification> notificationsAbout(RecordedEvent recorded, Instant sentAt) {
        Objects.requireNonNull(sentAt, "sent at");
        DomainEvent event = Objects.requireNonNull(recorded, "recorded event").event();
        if (!concern(event)) {
            return List.of();
        }
        return Revisions.inForce(plans.require(event.expeditionId()), plans.all()).charter().responsibles().stream()
                .map(recipient -> new Notification(recorded.id(), recipient, EventType.of(event), event.expeditionId(), sentAt))
                .toList();
    }
}
