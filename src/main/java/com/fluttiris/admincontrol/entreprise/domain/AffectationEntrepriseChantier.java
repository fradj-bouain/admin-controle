package com.fluttiris.admincontrol.entreprise.domain;

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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Table pivot Entreprise <-> Chantier. Porte le rôle de l'entreprise
 * ET son rattachement hiérarchique, tous deux propres à CE chantier :
 * une entreprise Principale sur le chantier A peut être STT1 sur le chantier B.
 * Une même entreprise peut aussi avoir plusieurs affectations SUR LE MÊME chantier
 * (ex : Principale ET STT1), à condition que ce ne soit jamais deux fois le même rôle
 * (unicité vérifiée sur le triplet chantier/entreprise/rôle, pas juste chantier/entreprise).
 *
 * affectationParenteId référence une autre ligne de cette même table (pas
 * directement une Entreprise) : le lien "qui a invité qui" n'a de sens que
 * dans le contexte d'un chantier donné.
 */
@Entity
@Table(name = "affectation_entreprise_chantier")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AffectationEntrepriseChantier extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "chantier_id", nullable = false)
    private UUID chantierId;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleEntreprise role;

    @Column(name = "affectation_parente_id")
    private UUID affectationParenteId;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut = LocalDate.now();

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(nullable = false)
    private String statut = "ACTIF";

    /**
     * @param parente l'affectation du parent SUR CE MÊME CHANTIER, requise pour STT1/STT2,
     *                interdite pour PRINCIPALE. La cohérence hiérarchique (STT1 doit avoir
     *                un parent PRINCIPALE, STT2 doit avoir un parent STT1) est vérifiée ici
     *                plutôt qu'en base, car elle nécessite de charger l'entité parente.
     */
    public static AffectationEntrepriseChantier creer(UUID chantierId, UUID entrepriseId,
                                                        RoleEntreprise role,
                                                        AffectationEntrepriseChantier parente) {
        validerHierarchie(role, parente);

        AffectationEntrepriseChantier affectation = new AffectationEntrepriseChantier();
        affectation.chantierId = chantierId;
        affectation.entrepriseId = entrepriseId;
        affectation.role = role;
        affectation.affectationParenteId = parente != null ? parente.getId() : null;
        return affectation;
    }

    private static void validerHierarchie(RoleEntreprise role, AffectationEntrepriseChantier parente) {
        if (role == RoleEntreprise.PRINCIPALE && parente != null) {
            throw new BusinessRuleViolationException("Une entreprise Principale ne peut pas avoir de parent");
        }
        if (role == RoleEntreprise.STT1) {
            if (parente == null || parente.getRole() != RoleEntreprise.PRINCIPALE) {
                throw new BusinessRuleViolationException("Un STT1 doit être rattaché à l'entreprise Principale de ce chantier");
            }
        }
        if (role == RoleEntreprise.STT2) {
            if (parente == null || parente.getRole() != RoleEntreprise.STT1) {
                throw new BusinessRuleViolationException("Un STT2 doit être rattaché à un STT1 de ce chantier");
            }
        }
    }

    public void desactiver() {
        this.statut = "INACTIF";
        this.dateFin = LocalDate.now();
    }

    /** Rouvre une affectation précédemment désactivée — n'existait pas avant. Volontairement
        SANS cascade vers les sous-traitants désactivés en même temps qu'elle (voir
        AffectationEntrepriseChantierService.desactiver()) : chacun se réactive séparément,
        pour ne pas réintroduire un sous-traitant que l'ADMIN n'a pas explicitement validé. */
    public void reactiver() {
        this.statut = "ACTIF";
        this.dateFin = null;
    }

    public boolean estActive() {
        return "ACTIF".equals(statut);
    }

    /** Une Principale ou un STT1 peuvent gérer des sous-traitants ; un STT2 ne peut gérer que ses salariés. */
    public boolean peutGererSousTraitants() {
        return role == RoleEntreprise.PRINCIPALE || role == RoleEntreprise.STT1;
    }
}
