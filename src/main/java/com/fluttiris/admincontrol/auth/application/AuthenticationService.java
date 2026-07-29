package com.fluttiris.admincontrol.auth.application;

import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Duration TOKEN_TTL = Duration.ofHours(8);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Transactional(readOnly = true)
    public LoginResult login(String username, String rawPassword) {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
            .filter(Utilisateur::isActif)
            .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(rawPassword, utilisateur.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides");
        }

        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .issuer("admincontrol-api")
            .issuedAt(now)
            .expiresAt(now.plus(TOKEN_TTL))
            .subject(utilisateur.getId().toString())
            .claim("username", utilisateur.getUsername())
            .claim("roles", utilisateur.getRoles());

        if (utilisateur.getEntrepriseId() != null) {
            claims.claim("entreprise_id", utilisateur.getEntrepriseId().toString());
        }
        if (utilisateur.getClientId() != null) {
            claims.claim("client_id", utilisateur.getClientId().toString());
        }

        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build()))
            .getTokenValue();

        return new LoginResult(token, TOKEN_TTL.toSeconds());
    }

    public record LoginResult(String accessToken, long expiresInSeconds) {
    }
}
