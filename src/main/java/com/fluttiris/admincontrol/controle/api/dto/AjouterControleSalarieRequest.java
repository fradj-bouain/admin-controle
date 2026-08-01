package com.fluttiris.admincontrol.controle.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AjouterControleSalarieRequest(
    @NotNull UUID salarieId,
    @NotNull UUID entrepriseId,
    boolean accorde,
    UUID actionCorrectiveId
) {
}
