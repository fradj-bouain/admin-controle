package com.fluttiris.admincontrol.client.api.dto;

import com.fluttiris.admincontrol.client.domain.Client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
    UUID id,
    String raisonSociale,
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
    String responsableSignataireAgrement,
    boolean actif,
    Instant createdAt
) {
    public static ClientResponse from(Client c) {
        return new ClientResponse(c.getId(), c.getRaisonSociale(), c.getAdresse(), c.getAdresse2(), c.getAdresse3(),
            c.getCodePostal(), c.getVille(), c.getPaysId(), c.getTelephone(), c.getTelephone2(), c.getTelephone3(),
            c.getFax(), c.getEmail(), c.getEmail2(), c.getEmail3(), c.getFormeJuridique(), c.getSiren(),
            c.getSiret(), c.getRcsRci(), c.getTvaIntra(), c.getNumCotisant(), c.getResponsableSignataireAgrement(),
            c.isActif(), c.getCreatedAt());
    }
}
