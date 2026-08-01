package com.fluttiris.admincontrol.controle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rapport_controle")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RapportControle {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "controle_id", nullable = false)
    private UUID controleId;

    @Column(name = "nb_salaries_controles", nullable = false)
    private int nbSalariesControles;

    @Column(name = "nb_accords", nullable = false)
    private int nbAccords;

    @Column(name = "nb_refus", nullable = false)
    private int nbRefus;

    @Column(name = "nb_nouvelles_entreprises", nullable = false)
    private int nbNouvellesEntreprises;

    @Column(name = "nb_nouveaux_salaries", nullable = false)
    private int nbNouveauxSalaries;

    @Column(name = "nb_entreprises", nullable = false)
    private int nbEntreprises;

    @Column(name = "nb_salaries_detaches", nullable = false)
    private int nbSalariesDetaches;

    @Column(name = "responsable_utilisateur_id")
    private UUID responsableUtilisateurId;

    @Column(name = "date_envoi")
    private Instant dateEnvoi;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static RapportControle creer(UUID controleId, int nbSalariesControles, int nbAccords, int nbRefus,
                                         int nbNouvellesEntreprises, int nbNouveauxSalaries, int nbEntreprises,
                                         int nbSalariesDetaches, UUID responsableUtilisateurId) {
        RapportControle rapport = new RapportControle();
        rapport.controleId = controleId;
        rapport.nbSalariesControles = nbSalariesControles;
        rapport.nbAccords = nbAccords;
        rapport.nbRefus = nbRefus;
        rapport.nbNouvellesEntreprises = nbNouvellesEntreprises;
        rapport.nbNouveauxSalaries = nbNouveauxSalaries;
        rapport.nbEntreprises = nbEntreprises;
        rapport.nbSalariesDetaches = nbSalariesDetaches;
        rapport.responsableUtilisateurId = responsableUtilisateurId;
        return rapport;
    }

    public void marquerEnvoye() {
        this.dateEnvoi = Instant.now();
    }

    // nbSalariesControles/nbAccords/nbRefus/nbEntreprises sont dérivés de la
    // checklist (controle_salarie) à la génération — pas de valeur métier à les
    // rouvrir à l'édition, contrairement aux compteurs saisis manuellement.
    public void modifier(int nbNouvellesEntreprises, int nbNouveauxSalaries, int nbSalariesDetaches, UUID responsableUtilisateurId) {
        this.nbNouvellesEntreprises = nbNouvellesEntreprises;
        this.nbNouveauxSalaries = nbNouveauxSalaries;
        this.nbSalariesDetaches = nbSalariesDetaches;
        this.responsableUtilisateurId = responsableUtilisateurId;
    }

    public void supprimer() {
        this.deletedAt = Instant.now();
    }
}
