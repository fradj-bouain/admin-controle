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
    UUID controleTiersId,
    boolean actif,
    // Uniquement pertinent pour un compte CLIENT (voir audit UX : la restriction
    // "aucune assignation = aucun accès" était invisible à l'écran). 0 par défaut
    // pour les autres rôles / les endpoints qui ne calculent pas ce compte.
    int nbChantiersAssignes,
    // Uniquement pertinent pour un compte CLIENT : true = voit tous les chantiers du
    // Client, false = "responsable de chantier" cantonné à nbChantiersAssignes.
    boolean accesTousChantiers
) {
    public static UtilisateurResponse from(Utilisateur u) {
        return from(u, 0);
    }

    public static UtilisateurResponse from(Utilisateur u, int nbChantiersAssignes) {
        return new UtilisateurResponse(u.getId(), u.getUsername(), u.getCivilite(), u.getNom(), u.getPrenom(),
            u.getEmail(), u.getRoles(), u.getEntrepriseId(), u.getClientId(), u.getControleTiersId(), u.isActif(),
            nbChantiersAssignes, u.isAccesTousChantiers());
    }
}
