package edu.itba.fieldops.adapters.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermitJpaRepository extends JpaRepository<PermitEntity, UUID> {
}
