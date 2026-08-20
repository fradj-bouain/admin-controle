package com.fluttiris.admincontrol.document.api.dto;

import com.fluttiris.admincontrol.document.domain.DocumentChantierSupplementaire;

import java.util.UUID;

public record DocumentChantierSupplementaireResponse(
    UUID id,
    UUID typeDocumentId,
    UUID chantierId
) {
    public static DocumentChantierSupplementaireResponse from(DocumentChantierSupplementaire r) {
        return new DocumentChantierSupplementaireResponse(r.getId(), r.getTypeDocumentId(), r.getChantierId());
    }
}
