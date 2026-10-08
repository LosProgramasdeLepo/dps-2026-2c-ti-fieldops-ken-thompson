package edu.itba.fieldops.adapters.jpa.expedition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpeditionJpaRepository extends JpaRepository<ExpeditionEntity, UUID> {
    @Query("""
            select e from ExpeditionEntity e
            join ExpeditionRegistrationEntity r on r.expeditionId = e.id
            where e.id = :id
            """)
    Optional<ExpeditionEntity> findRegisteredById(UUID id);

    @Query("""
            select e from ExpeditionEntity e
            join ExpeditionRegistrationEntity r on r.expeditionId = e.id
            order by r.registrationOrder
            """)
    List<ExpeditionEntity> findAllRegistered();

    @Query(value = """
            select e from ExpeditionEntity e
            join ExpeditionRegistrationEntity r on r.expeditionId = e.id
            order by r.registrationOrder
            """, countQuery = "select count(r) from ExpeditionRegistrationEntity r")
    Page<ExpeditionEntity> findAllRegistered(Pageable pageable);
}
