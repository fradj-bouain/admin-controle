package com.fluttiris.admincontrol.chantier.api.dto;

import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.RecurrenceControles;
import com.fluttiris.admincontrol.chantier.domain.StatutChantier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ChantierResponse(
    UUID id,
    String nom,
    UUID clientId,
    String prestation,
    String adresse,
    String adresse2,
    String adresse3,
    String codePostal,
    String ville,
    UUID paysId,
    UUID chefChantierUtilisateurId,
    UUID salarieResponsableId,
    String noteInterne,
    String noteClient,
    String ipsAllowed,
    LocalDate dateDebut,
    LocalDate dateFinPrevue,
    StatutChantier statut,
    RecurrenceControles recurrenceControles,
    LocalDate dateProchainControle,
    Instant createdAt
) {
    public static ChantierResponse from(Chantier chantier) {
        return new ChantierResponse(
            chantier.getId(),
            chantier.getNom(),
            chantier.getClientId(),
            chantier.getPrestation(),
            chantier.getAdresse(),
            chantier.getAdresse2(),
            chantier.getAdresse3(),
            chantier.getCodePostal(),
            chantier.getVille(),
            chantier.getPaysId(),
            chantier.getChefChantierUtilisateurId(),
            chantier.getSalarieResponsableId(),
            chantier.getNoteInterne(),
            chantier.getNoteClient(),
            chantier.getIpsAllowed(),
            chantier.getDateDebut(),
            chantier.getDateFinPrevue(),
            chantier.getStatut(),
            chantier.getRecurrenceControles(),
            chantier.getDateProchainControle(),
            chantier.getCreatedAt()
        );
    }
}
