package com.fluttiris.admincontrol.document.api.dto;

import com.fluttiris.admincontrol.document.domain.Document;
import com.fluttiris.admincontrol.document.domain.StatutValidation;

import java.time.LocalDate;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    UUID typeDocumentId,
    UUID salarieId,
    UUID entrepriseId,
    UUID chantierId,
    String fichierUrl,
    String nomFichierOriginal,
    String typeMime,
    Long tailleOctets,
    LocalDate dateDebutValidite,
    LocalDate dateExpiration,
    LocalDate dateRelance,
    String mentions,
    StatutValidation statutValidation,
    UUID documentEtatId
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getTypeDocumentId(), d.getSalarieId(), d.getEntrepriseId(),
            d.getChantierId(), d.getFichierUrl(), d.getNomFichierOriginal(), d.getTypeMime(), d.getTailleOctets(),
            d.getDateDebutValidite(), d.getDateExpiration(), d.getDateRelance(), d.getMentions(),
            d.getStatutValidation(), d.getDocumentEtatId());
    }
}
