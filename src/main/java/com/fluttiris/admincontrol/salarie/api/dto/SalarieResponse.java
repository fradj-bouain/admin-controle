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
    StatutSalarie statut,
    // Nom du chantier sur lequel ce salarié est actuellement affecté (null = aucun,
    // "Disponible" côté affichage) — distinct du champ `statut` ci-dessus, qui est un
    // simple interrupteur administratif sans lien avec une affectation réelle.
    String chantierActuel
) {
    public static SalarieResponse from(Salarie s) {
        return from(s, null);
    }

    public static SalarieResponse from(Salarie s, String chantierActuel) {
        return new SalarieResponse(s.getId(), s.getNom(), s.getPrenom(), s.getDateNaissance(),
            s.getNationalitePaysId(), s.getEntrepriseEmployeurId(), s.getTypeSalarieId(),
            s.getTypeContratId(), s.getFonctionId(), s.getStatut(), chantierActuel);
    }
}
