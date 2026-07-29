package com.fluttiris.admincontrol.controle.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateControleRequest(
    @NotNull UUID chantierId,
    @NotNull LocalDate dateControle,
    String remarques,
    UUID controleTiersId,
    LocalDate dateFin,
    boolean termine
) {
}
