package com.fluttiris.admincontrol.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux informations de l'utilisateur authentifié, porté par le JWT auto-hébergé
 * (voir AuthenticationService/JwtConfig — pas de fournisseur d'identité externe).
 * "entreprise_id" / "client_id" / "controle_tiers_id" sont mutuellement exclusifs :
 * chacun n'est présent que pour le type de compte correspondant (Entreprise, Client,
 * Contrôleur) ; absents pour un compte interne (SUPER_ADMIN, ADMIN).
 */
@Component
public class CurrentUser {

    public UUID keycloakId() {
        return UUID.fromString(currentJwt().getSubject());
    }

    public Optional<UUID> entrepriseId() {
        String claim = currentJwt().getClaimAsString("entreprise_id");
        return Optional.ofNullable(claim).map(UUID::fromString);
    }

    public Optional<UUID> clientId() {
        String claim = currentJwt().getClaimAsString("client_id");
        return Optional.ofNullable(claim).map(UUID::fromString);
    }

    public Optional<UUID> controleTiersId() {
        String claim = currentJwt().getClaimAsString("controle_tiers_id");
        return Optional.ofNullable(claim).map(UUID::fromString);
    }

    private Jwt currentJwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Aucun utilisateur authentifié dans le contexte courant");
        }
        return jwtAuth.getToken();
    }
}
