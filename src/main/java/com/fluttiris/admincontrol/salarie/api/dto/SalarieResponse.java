package com.fluttiris.admincontrol.salarie.api.dto;

import com.fluttiris.admincontrol.salarie.domain.Salarie;
import com.fluttiris.admincontrol.salarie.domain.StatutSalarie;

import java.time.LocalDate;
import java.util.UUID;

public record SalarieResponse(
    UUID id,
    String nom,
    String prenom,
    LocalDate dateNaissance,
    UUID nationalitePaysId,
    UUID entrepriseEmployeurId,
    UUID typeSalarieId,
    UUID typeContratId,
    UUID fonctionId,
    StatutSalarie statut
) {
    public static SalarieResponse from(Salarie s) {
        return new SalarieResponse(s.getId(), s.getNom(), s.getPrenom(), s.getDateNaissance(),
            s.getNationalitePaysId(), s.getEntrepriseEmployeurId(), s.getTypeSalarieId(),
            s.getTypeContratId(), s.getFonctionId(), s.getStatut());
    }
}
