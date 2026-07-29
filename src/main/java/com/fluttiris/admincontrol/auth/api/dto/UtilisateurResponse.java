package com.fluttiris.admincontrol.auth.api.dto;

import com.fluttiris.admincontrol.auth.domain.Utilisateur;

import java.util.Set;
import java.util.UUID;

public record UtilisateurResponse(
    UUID id,
    String username,
    String civilite,
    String nom,
    String prenom,
    String email,
    Set<String> roles,
    UUID entrepriseId,
    UUID clientId,
    boolean actif
) {
    public static UtilisateurResponse from(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getUsername(), u.getCivilite(), u.getNom(), u.getPrenom(),
            u.getEmail(), u.getRoles(), u.getEntrepriseId(), u.getClientId(), u.isActif());
    }
}
