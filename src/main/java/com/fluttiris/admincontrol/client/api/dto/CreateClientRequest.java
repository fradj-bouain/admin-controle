package com.fluttiris.admincontrol.client.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateClientRequest(
    @NotBlank String raisonSociale,
    String adresse,
    String adresse2,
    String adresse3,
    String codePostal,
    String ville,
    UUID paysId,
    String telephone,
    String telephone2,
    String telephone3,
    String fax,
    String email,
    String email2,
    String email3,
    String formeJuridique,
    String siren,
    String siret,
    String rcsRci,
    String tvaIntra,
    String numCotisant,
    String responsableSignataireAgrement
) {
}
