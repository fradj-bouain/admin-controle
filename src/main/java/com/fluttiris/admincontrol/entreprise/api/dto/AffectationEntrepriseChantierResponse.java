package com.fluttiris.admincontrol.entreprise.api.dto;

import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import com.fluttiris.admincontrol.entreprise.domain.RoleEntreprise;

import java.time.LocalDate;
import java.util.UUID;

public record AffectationEntrepriseChantierResponse(
    UUID id,
    UUID chantierId,
    UUID entrepriseId,
    RoleEntreprise role,
    UUID affectationParenteId,
    LocalDate dateDebut,
    LocalDate dateFin,
    String statut
) {
    public static AffectationEntrepriseChantierResponse from(AffectationEntrepriseChantier a) {
        return new AffectationEntrepriseChantierResponse(a.getId(), a.getChantierId(), a.getEntrepriseId(),
            a.getRole(), a.getAffectationParenteId(), a.getDateDebut(), a.getDateFin(), a.getStatut());
    }
}
