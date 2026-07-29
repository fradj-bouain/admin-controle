package com.fluttiris.admincontrol.configuration.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePaysRequest(@NotBlank String codeIso, @NotBlank String nom, String zone) {
}
