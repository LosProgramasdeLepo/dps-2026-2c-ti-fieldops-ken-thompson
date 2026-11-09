package edu.itba.fieldops.adapters.jpa.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, NotificationKey> {
    @Query("""
            select notification from NotificationEntity notification
            where notification.id.personId = :personId
            order by notification.registrationOrder desc
            """)
    Page<NotificationEntity> findOf(UUID personId, Pageable page);
}
