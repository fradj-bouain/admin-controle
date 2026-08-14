package com.fluttiris.admincontrol.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record CreateUtilisateurRequest(
    @NotBlank String username,
    @NotBlank String password,
    String civilite,
    @NotBlank String nom,
    @NotBlank String prenom,
    @NotBlank String email,
    @NotEmpty Set<String> roles,
    UUID entrepriseId,
    UUID clientId,
    UUID controleTiersId,
    // Uniquement appliqué pour un compte CLIENT créé par un SUPER_ADMIN : ignoré côté
    // serveur pour toute création en auto-gestion (Mon équipe), voir UtilisateurController.
    boolean accesTousChantiers
) {
}
