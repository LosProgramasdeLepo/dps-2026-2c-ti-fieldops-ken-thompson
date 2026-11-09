package edu.itba.fieldops.adapters.jpa.event;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventEntity, UUID>, JpaSpecificationExecutor<EventEntity> {
    @Query("""
            select event from EventEntity event
            where not exists (select 1 from EventFanoutEntity fanout where fanout.eventId = event.id)
            order by event.sequence
            """)
    List<EventEntity> findUndispatched(Limit limit);
}
