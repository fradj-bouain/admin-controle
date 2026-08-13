package com.fluttiris.admincontrol.entreprise.api.dto;

import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import com.fluttiris.admincontrol.entreprise.domain.RoleEntreprise;

import java.time.LocalDate;
import java.util.UUID;

public record AffectationEntrepriseChantierResponse(
    UUID id,
    UUID chantierId,
    UUID entrepriseId,
    // Raison sociale de l'entreprise affectée — permet d'afficher les sous-traitants d'un
    // chantier sans que le front doive rappeler /entreprises/{id} (une Entreprise n'a de
    // toute façon pas le droit de consulter la fiche d'une autre entreprise, voir
    // EntrepriseController.obtenir). Null quand non résolue (voir from(a) sans map).
    String raisonSocialeEntreprise,
    RoleEntreprise role,
    UUID affectationParenteId,
    LocalDate dateDebut,
    LocalDate dateFin,
    String statut
) {
    public static AffectationEntrepriseChantierResponse from(AffectationEntrepriseChantier a) {
        return from(a, null);
    }

    public static AffectationEntrepriseChantierResponse from(AffectationEntrepriseChantier a, String raisonSocialeEntreprise) {
        return new AffectationEntrepriseChantierResponse(a.getId(), a.getChantierId(), a.getEntrepriseId(),
            raisonSocialeEntreprise, a.getRole(), a.getAffectationParenteId(), a.getDateDebut(), a.getDateFin(), a.getStatut());
    }
}
