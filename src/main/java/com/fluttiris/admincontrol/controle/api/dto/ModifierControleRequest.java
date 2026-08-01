package com.fluttiris.admincontrol.controle.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ModifierControleRequest(
    @NotNull LocalDate dateControle,
    String remarques,
    UUID controleTiersId,
    LocalDate dateFin,
    boolean termine
) {
}
