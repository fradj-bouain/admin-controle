package com.fluttiris.admincontrol.configuration.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSalarieFonctionRequest(@NotBlank String libelle) {
}
