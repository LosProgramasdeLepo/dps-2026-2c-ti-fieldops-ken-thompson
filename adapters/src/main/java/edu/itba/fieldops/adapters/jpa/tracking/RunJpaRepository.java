package edu.itba.fieldops.adapters.jpa.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RunJpaRepository extends JpaRepository<RunEntity, UUID> {
}
