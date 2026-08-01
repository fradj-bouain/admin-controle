package com.fluttiris.admincontrol.document.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentEnAttenteResponse(
    UUID documentId,
    String typeLibelle,
    UUID salarieId,
    String salarieNom,
    UUID entrepriseId,
    String entrepriseNom,
    Instant createdAt
) {
}
