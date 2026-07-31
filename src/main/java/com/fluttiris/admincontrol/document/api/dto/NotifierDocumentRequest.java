package com.fluttiris.admincontrol.document.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotifierDocumentRequest(
    @NotBlank @Email String email,
    @NotBlank String sujet,
    @NotBlank String description
) {
}
