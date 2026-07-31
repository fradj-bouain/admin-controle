package com.fluttiris.admincontrol.messagerie.domain;

import com.fluttiris.admincontrol.common.audit.Auditable;
import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "regle_automatisation")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegleAutomatisation extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_declencheur", nullable = false)
    private TypeDeclencheur typeDeclencheur;

    /**
     * Id d'un ChampSurveillable enregistré (voir ChampSurveillableRegistry) —
     * uniquement renseigné quand typeDeclencheur == CHAMP_SURVEILLABLE ;
     * l'existence de la valeur est vérifiée par RegleAutomatisationService au
     * moment de la création/modification, contre la liste vivante des champs
     * surveillables disponibles.
     */
    @Column(name = "champ_surveillable_id")
    private String champSurveillableId;

    /**
     * Sens dépend de typeDeclencheur : nombre de jours avant l'échéance pour
     * CHAMP_SURVEILLABLE, intervalle de récurrence en jours pour PERIODIQUE,
     * sans effet pour les autres types (déclenchement immédiat ou manuel).
     */
    @Column(name = "nb_jours_avant", nullable = false)
    private int nbJoursAvant;

    @Column(nullable = false)
    private boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "cible_groupe", nullable = false)
    private CibleGroupe cibleGroupe;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinataire_type")
    private DestinataireType destinataireType;

    @Column(name = "destinataire_id")
    private UUID destinataireId;

    @Column(nullable = false)
    private String sujet;

    @Column(nullable = false)
    private String contenu;

    @Column(name = "numero_interne")
    private String numeroInterne;

    @Column(name = "titre_interne")
    private String titreInterne;

    /** Dernière date à laquelle une occurrence PERIODIQUE a été générée (sert à calculer la prochaine échéance). */
    @Column(name = "derniere_execution")
    private Instant derniereExecution;

    @Column(name = "nb_envois", nullable = false)
    private int nbEnvois = 0;

    @Column(name = "dernier_envoi")
    private Instant dernierEnvoi;

    public static RegleAutomatisation creer(String nom, TypeDeclencheur typeDeclencheur, String champSurveillableId,
                                             int nbJoursAvant, CibleGroupe cibleGroupe, DestinataireType destinataireType,
                                             UUID destinataireId, String sujet, String contenu, String numeroInterne,
                                             String titreInterne) {
        valider(typeDeclencheur, champSurveillableId, cibleGroupe, destinataireType, destinataireId);
        RegleAutomatisation regle = new RegleAutomatisation();
        regle.nom = nom;
        regle.typeDeclencheur = typeDeclencheur;
        regle.champSurveillableId = champSurveillableId;
        regle.nbJoursAvant = nbJoursAvant;
        regle.cibleGroupe = cibleGroupe;
        regle.destinataireType = destinataireType;
        regle.destinataireId = destinataireId;
        regle.sujet = sujet;
        regle.contenu = contenu;
        regle.numeroInterne = numeroInterne;
        regle.titreInterne = titreInterne;
        return regle;
    }

    public void modifier(String nom, TypeDeclencheur typeDeclencheur, String champSurveillableId, int nbJoursAvant,
                          CibleGroupe cibleGroupe, DestinataireType destinataireType, UUID destinataireId,
                          String sujet, String contenu, String numeroInterne, String titreInterne) {
        valider(typeDeclencheur, champSurveillableId, cibleGroupe, destinataireType, destinataireId);
        this.nom = nom;
        this.typeDeclencheur = typeDeclencheur;
        this.champSurveillableId = champSurveillableId;
        this.nbJoursAvant = nbJoursAvant;
        this.cibleGroupe = cibleGroupe;
        this.destinataireType = destinataireType;
        this.destinataireId = destinataireId;
        this.sujet = sujet;
        this.contenu = contenu;
        this.numeroInterne = numeroInterne;
        this.titreInterne = titreInterne;
    }

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    /** Appelé quand un message issu de cette règle vient d'être envoyé (compteur affiché en liste, comme le legacy). */
    public void marquerEnvoyee() {
        this.nbEnvois++;
        this.dernierEnvoi = Instant.now();
    }

    /** Appelé après génération d'une occurrence PERIODIQUE, pour calculer la prochaine échéance. */
    public void marquerExecutee() {
        this.derniereExecution = Instant.now();
    }

    public boolean periodiqueDue(LocalDate aujourdHui) {
        if (derniereExecution == null) {
            return true;
        }
        LocalDate derniere = derniereExecution.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        return !aujourdHui.isBefore(derniere.plusDays(nbJoursAvant));
    }

    private static void valider(TypeDeclencheur typeDeclencheur, String champSurveillableId, CibleGroupe cibleGroupe,
                                 DestinataireType destinataireType, UUID destinataireId) {
        if (typeDeclencheur == TypeDeclencheur.CHAMP_SURVEILLABLE && (champSurveillableId == null || champSurveillableId.isBlank())) {
            throw new BusinessRuleViolationException(
                "Une règle déclenchée par un champ surveillable doit préciser lequel");
        }
        if (typeDeclencheur != TypeDeclencheur.CHAMP_SURVEILLABLE && champSurveillableId != null && !champSurveillableId.isBlank()) {
            throw new BusinessRuleViolationException(
                "Le champ surveillable ne s'applique qu'aux règles déclenchées par un champ surveillable");
        }
        boolean destinataireRenseigne = destinataireType != null && destinataireId != null;
        if (cibleGroupe == CibleGroupe.SPECIFIQUE && !destinataireRenseigne) {
            throw new BusinessRuleViolationException(
                "Une règle ciblant un destinataire spécifique doit préciser le type et l'identifiant du destinataire");
        }
        if (cibleGroupe != CibleGroupe.SPECIFIQUE && destinataireRenseigne) {
            throw new BusinessRuleViolationException(
                "Une règle ciblant un groupe ne doit pas préciser de destinataire spécifique");
        }
    }
}
