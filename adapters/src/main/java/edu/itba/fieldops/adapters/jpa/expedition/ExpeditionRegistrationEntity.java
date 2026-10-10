package edu.itba.fieldops.adapters.jpa.expedition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "registered_expeditions")
public class ExpeditionRegistrationEntity {
    @Id
    @Column(name = "expedition_id")
    private UUID expeditionId;

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected ExpeditionRegistrationEntity() {
    }

    ExpeditionRegistrationEntity(UUID expeditionId) {
        this.expeditionId = expeditionId;
    }
}
