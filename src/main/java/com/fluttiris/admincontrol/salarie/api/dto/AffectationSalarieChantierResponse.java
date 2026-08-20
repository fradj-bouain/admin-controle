package com.fluttiris.admincontrol.salarie.api.dto;

import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantier;
import com.fluttiris.admincontrol.salarie.domain.StatutAcces;

import java.time.LocalDate;
import java.util.UUID;

public record AffectationSalarieChantierResponse(
    UUID id,
    UUID salarieId,
    // Nom du salarié — résolu en lot pour la liste transverse (voir SalarieController,
    // GET /salaries/affectations) ; null sur les réponses qui ne le fournissent pas (le
    // salarié est déjà connu, ex: GET /salaries/{id}/chantiers).
    String nomSalarie,
    UUID chantierId,
    String nomChantier,
    UUID affectationEntrepriseChantierId,
    UUID entrepriseId,
    String nomEntreprise,
    LocalDate dateDebut,
    LocalDate dateFin,
    // Statut d'ENGAGEMENT ("travaille encore sur ce chantier ?"), distinct de statutAcces
    // ci-dessous ("son accès au site est-il en règle ?") — voir AffectationSalarieChantier.
    String statut,
    StatutAcces statutAcces,
    String motifRefus,
    boolean epiGants,
    boolean epiCasque,
    boolean epiChaussures,
    boolean badgeEdite,
    boolean present
) {
    public static AffectationSalarieChantierResponse from(AffectationSalarieChantier a, UUID entrepriseId) {
        return from(a, entrepriseId, null, null, null);
    }

    public static AffectationSalarieChantierResponse from(AffectationSalarieChantier a, UUID entrepriseId,
                                                            String nomSalarie, String nomChantier, String nomEntreprise) {
        return new AffectationSalarieChantierResponse(a.getId(), a.getSalarieId(), nomSalarie, a.getChantierId(),
            nomChantier, a.getAffectationEntrepriseChantierId(), entrepriseId, nomEntreprise, a.getDateDebut(),
            a.getDateFin(), a.getStatut(), a.getStatutAcces(), a.getMotifRefus(), a.isEpiGants(), a.isEpiCasque(),
            a.isEpiChaussures(), a.isBadgeEdite(), a.isPresent());
    }
}
