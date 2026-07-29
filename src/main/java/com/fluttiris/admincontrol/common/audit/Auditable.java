package com.fluttiris.admincontrol.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base commune pour la traçabilité (qui/quand) exigée par le cahier des charges
 * sur les actions critiques (création, modification, suppression logique).
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Suppression logique : la ligne reste en base (traçabilité, cohérence des FK
     * vers les tables filles) mais disparaît de toutes les lectures via
     * {@code @SQLRestriction("deleted_at IS NULL")} sur chaque entité concernée.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void supprimer() {
        this.deletedAt = Instant.now();
    }

    public boolean estSupprime() {
        return deletedAt != null;
    }
}
