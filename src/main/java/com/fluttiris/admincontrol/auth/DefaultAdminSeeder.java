package com.fluttiris.admincontrol.auth;

import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Crée un compte SUPER_ADMIN de démarrage, pour permettre une première connexion sans
 * étape manuelle (utile en dev local sans IdP externe à provisionner).
 *
 * Idempotent PAR USERNAME (même idiome que TestAccountsSeeder), pas par "table utilisateur
 * vide" — l'ancienne condition (count() == 0) dépendait de l'ordre d'exécution des
 * ApplicationRunner, non garanti par Spring : si TestAccountsSeeder tournait avant celui-ci,
 * la table n'était déjà plus vide et admin.local n'était jamais créé. Vérifié en conditions
 * réelles sur un schéma fraîchement migré (les deux ordres d'exécution sont possibles).
 */
@Component
@RequiredArgsConstructor
public class DefaultAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminSeeder.class);
    private static final String USERNAME = "admin.local";

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (utilisateurRepository.existsByUsername(USERNAME)) {
            return;
        }

        Utilisateur admin = Utilisateur.creer(
            USERNAME,
            passwordEncoder.encode("admin"),
            null,
            "Administrateur",
            "Système",
            "admin@admincontrol.local",
            Set.of("SUPER_ADMIN"),
            null,
            null,
            null
        );
        utilisateurRepository.save(admin);
        log.warn("Compte de démarrage créé (username={}, password=admin). "
            + "À changer avant tout déploiement au-delà du poste de développement.", USERNAME);
    }
}
