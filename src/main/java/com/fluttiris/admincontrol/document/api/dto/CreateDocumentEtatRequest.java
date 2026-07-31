package com.fluttiris.admincontrol.document.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentEtatRequest(
    @NotBlank String titre,
    boolean parDefaut,
    boolean dateExpiree,
    boolean valideLeDocument
) {
}
