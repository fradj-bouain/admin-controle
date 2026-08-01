package com.fluttiris.admincontrol.controle.api.dto;

import java.util.UUID;

public record ModifierRapportRequest(
    int nbNouvellesEntreprises,
    int nbNouveauxSalaries,
    int nbSalariesDetaches,
    UUID responsableUtilisateurId
) {
}
