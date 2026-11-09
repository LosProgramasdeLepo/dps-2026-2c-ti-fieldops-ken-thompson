package edu.itba.fieldops.adapters.jpa.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventFanoutJpaRepository extends JpaRepository<EventFanoutEntity, UUID> {
}
