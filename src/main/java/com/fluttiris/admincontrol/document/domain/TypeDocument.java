package com.fluttiris.admincontrol.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

/**
 * Modèle de document configurable (titre, cible, obligatoire ou non...) —
 * exigence client : les documents à envoyer sont paramétrables, pas figés en dur.
 */
@Entity
@Table(name = "type_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TypeDocument {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CibleDocument cible;

    @Column(nullable = false)
    private boolean obligatoire = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormatDocument format = FormatDocument.PDF;

    @Column(name = "corps_de_metier_id")
    private UUID corpsDeMetierId;

    @Column(name = "pays_id")
    private UUID paysId;

    /** Ce type de document exige-t-il une date de début de validité ? (ex: DPAE = non, carte pro = oui) */
    @Column(name = "date_debut_validite_requise", nullable = false)
    private boolean dateDebutValiditeRequise = true;

    @Column(name = "date_fin_validite_requise", nullable = false)
    private boolean dateFinValiditeRequise = true;

    @Column(name = "nb_jours_relance_avant", nullable = false)
    private int nbJoursRelanceAvant = 0;

    @Column(name = "nb_jours_recurrence", nullable = false)
    private int nbJoursRecurrence = 0;

    /** Un document expiré de ce type retire-t-il automatiquement l'accès au chantier ? */
    @Column(name = "retire_accord_acces", nullable = false)
    private boolean retireAccordAcces = true;

    public static TypeDocument creer(String libelle, CibleDocument cible, boolean obligatoire, FormatDocument format,
                                      UUID corpsDeMetierId, UUID paysId, boolean dateDebutValiditeRequise,
                                      boolean dateFinValiditeRequise, int nbJoursRelanceAvant,
                                      int nbJoursRecurrence, boolean retireAccordAcces) {
        TypeDocument type = new TypeDocument();
        type.libelle = libelle;
        type.cible = cible;
        type.obligatoire = obligatoire;
        type.format = format;
        type.corpsDeMetierId = corpsDeMetierId;
        type.paysId = paysId;
        type.dateDebutValiditeRequise = dateDebutValiditeRequise;
        type.dateFinValiditeRequise = dateFinValiditeRequise;
        type.nbJoursRelanceAvant = nbJoursRelanceAvant;
        type.nbJoursRecurrence = nbJoursRecurrence;
        type.retireAccordAcces = retireAccordAcces;
        return type;
    }

    public void modifier(String libelle, CibleDocument cible, boolean obligatoire, FormatDocument format,
                          UUID corpsDeMetierId, UUID paysId, boolean dateDebutValiditeRequise,
                          boolean dateFinValiditeRequise, int nbJoursRelanceAvant,
                          int nbJoursRecurrence, boolean retireAccordAcces) {
        this.libelle = libelle;
        this.cible = cible;
        this.obligatoire = obligatoire;
        this.format = format;
        this.corpsDeMetierId = corpsDeMetierId;
        this.paysId = paysId;
        this.dateDebutValiditeRequise = dateDebutValiditeRequise;
        this.dateFinValiditeRequise = dateFinValiditeRequise;
        this.nbJoursRelanceAvant = nbJoursRelanceAvant;
        this.nbJoursRecurrence = nbJoursRecurrence;
        this.retireAccordAcces = retireAccordAcces;
    }
}
