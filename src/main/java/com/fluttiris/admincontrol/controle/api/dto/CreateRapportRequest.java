package com.fluttiris.admincontrol.controle.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRapportRequest(
    @NotNull UUID controleId,
    int nbSalariesControles,
    int nbAccords,
    int nbRefus,
    int nbNouvellesEntreprises,
    int nbNouveauxSalaries,
    int nbEntreprises,
    int nbSalariesDetaches,
    UUID responsableUtilisateurId
) {
}
