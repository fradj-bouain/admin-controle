package com.fluttiris.admincontrol.entreprise.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateEntrepriseRequest(
    @NotBlank String raisonSociale,
    String siret,
    String adresse,
    String adresse2,
    String adresse3,
    String codePostal,
    String ville,
    UUID paysId,
    UUID corpsDeMetierId,
    String telephone,
    String telephone2,
    String telephone3,
    String fax,
    String email,
    String email2,
    String email3,
    String formeJuridique,
    String siren,
    String rcsRci,
    String tvaIntra,
    String numCotisant,
    String responsableSignataireAgrement,
    String commentaire
) {
}
