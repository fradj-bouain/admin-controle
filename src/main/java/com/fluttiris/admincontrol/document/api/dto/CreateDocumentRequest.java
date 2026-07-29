package com.fluttiris.admincontrol.document.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDocumentRequest(
    @NotNull UUID typeDocumentId,
    UUID salarieId,
    UUID entrepriseId,
    UUID chantierId,
    String fichierUrl,
    LocalDate dateDebutValidite,
    LocalDate dateExpiration,
    LocalDate dateRelance,
    String mentions
) {
}
