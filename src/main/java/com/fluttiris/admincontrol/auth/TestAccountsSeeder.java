package com.fluttiris.admincontrol.auth;

import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.client.domain.Client;
import com.fluttiris.admincontrol.client.domain.ClientRepository;
import com.fluttiris.admincontrol.configuration.domain.ControleTiers;
import com.fluttiris.admincontrol.configuration.domain.ControleTiersRepository;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Crée, si absents, un compte de test par rôle scopé (ENTREPRISE / CLIENT / CONTROLEUR),
 * chacun rattaché à une entité de test dédiée créée à la volée — pour pouvoir vérifier
 * manuellement le cloisonnement par permission (voir ScopeAuthorizationService) sans étape
 * manuelle. Idempotent PAR USERNAME (pas par "table utilisateur vide" comme DefaultAdminSeeder) :
 * tourne à chaque démarrage, ne recrée jamais un compte déjà présent.
 *
 * Mêmes réserves que DefaultAdminSeeder : mots de passe faibles et connus, à ne jamais
 * laisser survivre au-delà du poste de développement / d'un environnement de démo.
 */
@Component
@RequiredArgsConstructor
public class TestAccountsSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestAccountsSeeder.class);
    private static final String PASSWORD = "test1234";

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ClientRepository clientRepository;
    private final ControleTiersRepository controleTiersRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedEntreprise();
        seedClient();
        seedControleur();
    }

    private void seedEntreprise() {
        if (utilisateurRepository.existsByUsername("entreprise.test")) {
            return;
        }
        Entreprise entreprise = entrepriseRepository.save(Entreprise.creer(
            "Entreprise Test", null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null));
        creerCompte("entreprise.test", "Test", "Entreprise", "entreprise.test@admincontrol.local",
            Set.of("ENTREPRISE"), entreprise.getId(), null, null);
        log.warn("Compte de test créé : username=entreprise.test password={} (rôle ENTREPRISE, entrepriseId={})",
            PASSWORD, entreprise.getId());
    }

    private void seedClient() {
        if (utilisateurRepository.existsByUsername("client.test")) {
            return;
        }
        Client client = clientRepository.save(Client.creer(
            "Client Test", null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null));
        creerCompte("client.test", "Test", "Client", "client.test@admincontrol.local",
            Set.of("CLIENT"), null, client.getId(), null);
        log.warn("Compte de test créé : username=client.test password={} (rôle CLIENT, clientId={})",
            PASSWORD, client.getId());
    }

    private void seedControleur() {
        if (utilisateurRepository.existsByUsername("controleur.test")) {
            return;
        }
        ControleTiers organisme = controleTiersRepository.save(ControleTiers.creer("Organisme de Contrôle Test"));
        creerCompte("controleur.test", "Test", "Contrôleur", "controleur.test@admincontrol.local",
            Set.of("CONTROLEUR"), null, null, organisme.getId());
        log.warn("Compte de test créé : username=controleur.test password={} (rôle CONTROLEUR, controleTiersId={})",
            PASSWORD, organisme.getId());
    }

    private void creerCompte(String username, String nom, String prenom, String email, Set<String> roles,
                              UUID entrepriseId, UUID clientId, UUID controleTiersId) {
        Utilisateur utilisateur = Utilisateur.creer(username, passwordEncoder.encode(PASSWORD), null, nom, prenom,
            email, roles, entrepriseId, clientId, controleTiersId);
        utilisateurRepository.save(utilisateur);
    }
}
