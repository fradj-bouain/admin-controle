package com.fluttiris.admincontrol.salarie.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AffecterSalarieRequest(
    @NotNull UUID salarieId,
    @NotNull UUID affectationEntrepriseChantierId
) {
}
