package com.fluttiris.admincontrol.chantier.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.time.Instant;
import java.util.UUID;

/**
 * Accès d'un utilisateur (côté client) à un chantier précis — brique nécessaire
 * pour un futur portail client en lecture seule. Pas de FK déclarée vers
 * utilisateur (module auth séparé), cohérent avec le reste du schéma.
 */
@Entity
@Table(name = "chantier_utilisateur")
@IdClass(ChantierUtilisateurId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChantierUtilisateur {

    @Id
    @Column(name = "chantier_id")
    private UUID chantierId;

    @Id
    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ChantierUtilisateur creer(UUID chantierId, UUID utilisateurId) {
        ChantierUtilisateur acces = new ChantierUtilisateur();
        acces.chantierId = chantierId;
        acces.utilisateurId = utilisateurId;
        return acces;
    }
}
