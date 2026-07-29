package com.fluttiris.admincontrol.controle.api.dto;

import com.fluttiris.admincontrol.controle.domain.RapportControle;

import java.time.Instant;
import java.util.UUID;

public record RapportControleResponse(
    UUID id,
    UUID controleId,
    int nbSalariesControles,
    int nbAccords,
    int nbRefus,
    int nbNouvellesEntreprises,
    int nbNouveauxSalaries,
    int nbEntreprises,
    int nbSalariesDetaches,
    UUID responsableUtilisateurId,
    Instant dateEnvoi
) {
    public static RapportControleResponse from(RapportControle r) {
        return new RapportControleResponse(r.getId(), r.getControleId(), r.getNbSalariesControles(),
            r.getNbAccords(), r.getNbRefus(), r.getNbNouvellesEntreprises(), r.getNbNouveauxSalaries(),
            r.getNbEntreprises(), r.getNbSalariesDetaches(), r.getResponsableUtilisateurId(), r.getDateEnvoi());
    }
}
