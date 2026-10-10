package edu.itba.fieldops.adapters.jpa.expedition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExpeditionRegistrationJpaRepository extends JpaRepository<ExpeditionRegistrationEntity, UUID> {
}
