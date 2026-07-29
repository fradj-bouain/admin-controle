package com.fluttiris.admincontrol.salarie.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSalarieRequest(
    @NotBlank String nom,
    @NotBlank String prenom,
    LocalDate dateNaissance,
    UUID nationalitePaysId,
    @NotNull UUID entrepriseEmployeurId,
    UUID typeSalarieId,
    UUID typeContratId,
    UUID fonctionId
) {
}
