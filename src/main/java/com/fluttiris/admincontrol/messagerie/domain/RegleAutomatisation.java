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
    @Column(name = "evenement_declencheur", nullable = false)
    private EvenementDeclencheur evenementDeclencheur;

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

    public static RegleAutomatisation creer(String nom, EvenementDeclencheur evenementDeclencheur, int nbJoursAvant,
                                             CibleGroupe cibleGroupe, DestinataireType destinataireType,
                                             UUID destinataireId, String sujet, String contenu) {
        valider(cibleGroupe, destinataireType, destinataireId);
        RegleAutomatisation regle = new RegleAutomatisation();
        regle.nom = nom;
        regle.evenementDeclencheur = evenementDeclencheur;
        regle.nbJoursAvant = nbJoursAvant;
        regle.cibleGroupe = cibleGroupe;
        regle.destinataireType = destinataireType;
        regle.destinataireId = destinataireId;
        regle.sujet = sujet;
        regle.contenu = contenu;
        return regle;
    }

    public void modifier(String nom, EvenementDeclencheur evenementDeclencheur, int nbJoursAvant,
                          CibleGroupe cibleGroupe, DestinataireType destinataireType, UUID destinataireId,
                          String sujet, String contenu) {
        valider(cibleGroupe, destinataireType, destinataireId);
        this.nom = nom;
        this.evenementDeclencheur = evenementDeclencheur;
        this.nbJoursAvant = nbJoursAvant;
        this.cibleGroupe = cibleGroupe;
        this.destinataireType = destinataireType;
        this.destinataireId = destinataireId;
        this.sujet = sujet;
        this.contenu = contenu;
    }

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    private static void valider(CibleGroupe cibleGroupe, DestinataireType destinataireType, UUID destinataireId) {
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
