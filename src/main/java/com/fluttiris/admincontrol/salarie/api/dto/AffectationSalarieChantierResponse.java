package com.fluttiris.admincontrol.salarie.api.dto;

import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantier;
import com.fluttiris.admincontrol.salarie.domain.StatutAcces;

import java.time.LocalDate;
import java.util.UUID;

public record AffectationSalarieChantierResponse(
    UUID id,
    UUID salarieId,
    UUID chantierId,
    UUID affectationEntrepriseChantierId,
    UUID entrepriseId,
    LocalDate dateDebut,
    LocalDate dateFin,
    StatutAcces statutAcces,
    String motifRefus,
    boolean epiGants,
    boolean epiCasque,
    boolean epiChaussures,
    boolean badgeEdite,
    boolean present
) {
    public static AffectationSalarieChantierResponse from(AffectationSalarieChantier a, UUID entrepriseId) {
        return new AffectationSalarieChantierResponse(a.getId(), a.getSalarieId(), a.getChantierId(),
            a.getAffectationEntrepriseChantierId(), entrepriseId, a.getDateDebut(), a.getDateFin(), a.getStatutAcces(),
            a.getMotifRefus(), a.isEpiGants(), a.isEpiCasque(), a.isEpiChaussures(), a.isBadgeEdite(), a.isPresent());
    }
}
