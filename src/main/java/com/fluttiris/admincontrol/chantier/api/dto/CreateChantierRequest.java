package com.fluttiris.admincontrol.chantier.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateChantierRequest(
    @NotBlank String nom,
    @NotNull UUID clientId,
    String prestation,
    String adresse,
    String adresse2,
    String adresse3,
    String codePostal,
    String ville,
    UUID paysId,
    String noteInterne,
    String noteClient,
    String ipsAllowed,
    LocalDate dateDebut,
    LocalDate dateFinPrevue
) {
}
