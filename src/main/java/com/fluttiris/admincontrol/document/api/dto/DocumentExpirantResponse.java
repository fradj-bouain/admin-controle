package com.fluttiris.admincontrol.document.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DocumentExpirantResponse(
    UUID documentId,
    UUID typeDocumentId,
    String typeLibelle,
    UUID salarieId,
    String salarieNom,
    UUID entrepriseId,
    String entrepriseNom,
    LocalDate dateExpiration
) {
}
