package com.fluttiris.admincontrol.document.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AjouterDocumentChantierSupplementaireRequest(
    @NotNull UUID typeDocumentId
) {
}
