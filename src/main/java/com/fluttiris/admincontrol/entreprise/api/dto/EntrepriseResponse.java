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
    boolean actif,
    // Nom du chantier sur lequel cette entreprise est actuellement affectée (null =
    // aucun, "Disponible" côté affichage) — distinct du champ `actif` ci-dessus, qui
    // est un simple interrupteur administratif sans lien avec une affectation réelle.
    String chantierActuel,
    // Rang(s) (PRINCIPALE/STT1/STT2) sur ses chantiers en cours — null = aucune affectation
    // active. Voir EntrepriseService.rangActuelParEntreprise (demande client, listing).
    String rangActuel,
    Instant createdAt
) {
    public static EntrepriseResponse from(Entreprise e) {
        return from(e, null, null);
    }

    public static EntrepriseResponse from(Entreprise e, String chantierActuel) {
        return from(e, chantierActuel, null);
    }

    public static EntrepriseResponse from(Entreprise e, String chantierActuel, String rangActuel) {
        return new EntrepriseResponse(e.getId(), e.getRaisonSociale(), e.getSiret(), e.getAdresse(),
            e.getAdresse2(), e.getAdresse3(), e.getCodePostal(), e.getVille(), e.getPaysId(), e.getCorpsDeMetierId(),
            e.getTelephone(), e.getTelephone2(), e.getTelephone3(), e.getFax(), e.getEmail(), e.getEmail2(),
            e.getEmail3(), e.getFormeJuridique(), e.getSiren(), e.getRcsRci(), e.getTvaIntra(), e.getNumCotisant(),
            e.getResponsableSignataireAgrement(), e.getCommentaire(), e.getDateDesactivation(), e.isActif(),
            chantierActuel, rangActuel, e.getCreatedAt());
    }
}
