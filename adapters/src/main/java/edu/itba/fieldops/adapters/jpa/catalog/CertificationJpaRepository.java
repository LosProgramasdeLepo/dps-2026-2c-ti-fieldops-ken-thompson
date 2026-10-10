package edu.itba.fieldops.adapters.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CertificationJpaRepository extends JpaRepository<CertificationEntity, UUID> {
}
