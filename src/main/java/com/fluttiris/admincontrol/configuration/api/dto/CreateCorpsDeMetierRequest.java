package com.fluttiris.admincontrol.configuration.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCorpsDeMetierRequest(@NotBlank String libelle) {
}
