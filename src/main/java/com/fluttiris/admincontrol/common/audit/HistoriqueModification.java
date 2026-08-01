package com.fluttiris.admincontrol.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Journal d'audit générique (qui a fait quoi, quand, sur quelle entité) — table
 * présente depuis le schéma initial mais jamais câblée jusqu'ici. `details` est
 * une chaîne JSON libre, propre à chaque (entite, action) : on y capture les
 * informations utiles à l'affichage (ex: libellé du type de document) au moment
 * de l'écriture, plutôt que de les résoudre par jointure à la lecture — l'entrée
 * d'audit reste lisible même si l'entité d'origine est ensuite supprimée.
 */
@Entity
@Table(name = "historique_modification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistoriqueModification {

    @Id
    private UUID id = UUID.randomUUID();

    private String entite;

    @Column(name = "entite_id")
    private UUID entiteId;

    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public static HistoriqueModification enregistrer(String entite, UUID entiteId, String action,
                                                       String detailsJson, UUID utilisateurId) {
        HistoriqueModification h = new HistoriqueModification();
        h.entite = entite;
        h.entiteId = entiteId;
        h.action = action;
        h.details = detailsJson;
        h.utilisateurId = utilisateurId;
        return h;
    }
}
