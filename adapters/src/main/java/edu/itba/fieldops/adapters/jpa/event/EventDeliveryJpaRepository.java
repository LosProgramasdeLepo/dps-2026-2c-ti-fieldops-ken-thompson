package edu.itba.fieldops.adapters.jpa.event;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventDeliveryJpaRepository extends JpaRepository<EventDeliveryEntity, DeliveryKey> {
    @Query("""
            select delivery from EventDeliveryEntity delivery, EventEntity event
            where event.id = delivery.id.eventId
              and delivery.status in ('PENDING', 'RETRYING')
              and delivery.nextAttemptAt <= :now
            order by event.sequence, delivery.id.consumer
            """)
    List<EventDeliveryEntity> findDue(Instant now, Limit limit);

    @Query("""
            select delivery from EventDeliveryEntity delivery
            where delivery.id.eventId = :eventId
            order by delivery.id.consumer
            """)
    List<EventDeliveryEntity> findOf(UUID eventId);
}
