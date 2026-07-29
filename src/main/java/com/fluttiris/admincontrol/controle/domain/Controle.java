package com.fluttiris.admincontrol.controle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "controle")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Controle {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "chantier_id", nullable = false)
    private UUID chantierId;

    @Column(name = "controleur_utilisateur_id", nullable = false)
    private UUID controleurUtilisateurId;

    @Column(name = "date_controle", nullable = false)
    private LocalDate dateControle;

    private String remarques;

    @Column(name = "controle_tiers_id")
    private UUID controleTiersId;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(nullable = false)
    private boolean termine = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static Controle creer(UUID chantierId, UUID controleurUtilisateurId, LocalDate dateControle,
                                  String remarques, UUID controleTiersId, LocalDate dateFin, boolean termine) {
        Controle controle = new Controle();
        controle.chantierId = chantierId;
        controle.controleurUtilisateurId = controleurUtilisateurId;
        controle.dateControle = dateControle;
        controle.remarques = remarques;
        controle.controleTiersId = controleTiersId;
        controle.dateFin = dateFin;
        controle.termine = termine;
        return controle;
    }

    public void supprimer() {
        this.deletedAt = Instant.now();
    }
}
