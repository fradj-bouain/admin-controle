package com.fluttiris.admincontrol.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

/** password volontairement sans @NotBlank : vide = mot de passe inchangé (voir
    UtilisateurService.modifier), contrairement à la création où il est obligatoire.
    accesTousChantiers est également ignoré côté serveur en auto-gestion (Mon équipe) :
    seul un SUPER_ADMIN peut le faire évoluer, voir UtilisateurController. */
public record ModifierUtilisateurRequest(
    @NotBlank String nom,
    @NotBlank String prenom,
    @NotBlank String email,
    @NotBlank String username,
    String password,
    Boolean accesTousChantiers
) {
}
