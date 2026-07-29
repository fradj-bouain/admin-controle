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
 * Crée un compte SUPER_ADMIN de démarrage si la table utilisateur est vide,
 * pour permettre une première connexion sans étape manuelle (utile en dev
 * local sans IdP externe à provisionner).
 */
@Component
@RequiredArgsConstructor
public class DefaultAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminSeeder.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (utilisateurRepository.count() > 0) {
            return;
        }

        Utilisateur admin = Utilisateur.creer(
            "admin.local",
            passwordEncoder.encode("admin"),
            null,
            "Administrateur",
            "Système",
            "admin@admincontrol.local",
            Set.of("SUPER_ADMIN"),
            null,
            null
        );
        utilisateurRepository.save(admin);
        log.warn("Aucun utilisateur trouvé : compte de démarrage créé (username=admin.local, password=admin). "
            + "À changer avant tout déploiement au-delà du poste de développement.");
    }
}
