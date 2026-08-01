package com.fluttiris.admincontrol.controle.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRapportRequest(
    @NotNull UUID controleId,
    int nbNouvellesEntreprises,
    int nbNouveauxSalaries,
    int nbSalariesDetaches,
    UUID responsableUtilisateurId
) {
}
