package com.fluttiris.admincontrol.chantier.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccorderAccesRequest(@NotNull UUID utilisateurId) {
}
