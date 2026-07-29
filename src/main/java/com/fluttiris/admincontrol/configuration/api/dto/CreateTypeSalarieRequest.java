package com.fluttiris.admincontrol.configuration.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTypeSalarieRequest(@NotBlank String code, @NotBlank String libelle) {
}
