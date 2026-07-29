package com.fluttiris.admincontrol.entreprise.api.dto;

import com.fluttiris.admincontrol.entreprise.domain.Entreprise;

import java.time.Instant;
import java.util.UUID;

public record EntrepriseResponse(
    UUID id,
    String raisonSociale,
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
    String commentaire,
    Instant dateDesactivation,
    boolean actif
) {
    public static EntrepriseResponse from(Entreprise e) {
        return new EntrepriseResponse(e.getId(), e.getRaisonSociale(), e.getSiret(), e.getAdresse(),
            e.getAdresse2(), e.getAdresse3(), e.getCodePostal(), e.getVille(), e.getPaysId(), e.getCorpsDeMetierId(),
            e.getTelephone(), e.getTelephone2(), e.getTelephone3(), e.getFax(), e.getEmail(), e.getEmail2(),
            e.getEmail3(), e.getFormeJuridique(), e.getSiren(), e.getRcsRci(), e.getTvaIntra(), e.getNumCotisant(),
            e.getResponsableSignataireAgrement(), e.getCommentaire(), e.getDateDesactivation(), e.isActif());
    }
}
