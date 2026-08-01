package com.fluttiris.admincontrol.configuration.api.dto;

import com.fluttiris.admincontrol.configuration.domain.CibleActionCorrective;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateActionCorrectiveRequest(@NotBlank String nom, @NotNull CibleActionCorrective cible) {
}
