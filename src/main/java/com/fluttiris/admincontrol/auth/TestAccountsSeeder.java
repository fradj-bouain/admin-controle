package com.fluttiris.admincontrol.auth;

import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.client.domain.Client;
import com.fluttiris.admincontrol.client.domain.ClientRepository;
import com.fluttiris.admincontrol.configuration.domain.ControleTiers;
import com.fluttiris.admincontrol.configuration.domain.ControleTiersRepository;
import com.fluttiris.admincontrol.configuration.domain.CorpsDeMetierRepository;
import com.fluttiris.admincontrol.configuration.domain.PaysRepository;
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
 * Crée, si absent, le compte SUPER_ADMIN de démarrage (admin.local) ainsi qu'un compte de
 * test par rôle scopé (ENTREPRISE / CLIENT / CONTROLEUR), chacun rattaché à une entité de
 * test dédiée créée à la volée — pour pouvoir vérifier manuellement le cloisonnement par
 * permission (voir ScopeAuthorizationService) sans étape manuelle, et se connecter sans
 * IdP externe à provisionner. Idempotent PAR USERNAME (pas "table utilisateur vide" — cette
 * ancienne condition, portée par un DefaultAdminSeeder séparé, dépendait de l'ordre
 * d'exécution des ApplicationRunner : si ce seeder-ci tournait en premier, admin.local
 * n'était jamais créé. Fusionné ici, même idiome pour les 4 comptes, plus de risque d'ordre) :
 * tourne à chaque démarrage, ne recrée jamais un compte déjà présent.
 *
 * Mots de passe faibles et connus, à ne jamais laisser survivre au-delà du poste de
 * développement / d'un environnement de démo.
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
    private final PaysRepository paysRepository;
    private final CorpsDeMetierRepository corpsDeMetierRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedEntreprise();
        seedClient();
        seedControleur();
    }

    private void seedAdmin() {
        String username = "admin.local";
        if (utilisateurRepository.existsByUsername(username)) {
            return;
        }
        Utilisateur admin = Utilisateur.creer(username, passwordEncoder.encode("admin"), null,
            "Administrateur", "Système", "admin@admincontrol.local", Set.of("SUPER_ADMIN"), null, null, null);
        utilisateurRepository.save(admin);
        log.warn("Compte de démarrage créé : username={} password=admin (rôle SUPER_ADMIN). "
            + "À changer avant tout déploiement au-delà du poste de développement.", username);
    }

    private void seedEntreprise() {
        if (utilisateurRepository.existsByUsername("entreprise.test")) {
            return;
        }
        // paysId/corpsDeMetierId résolus par leur clé métier (code ISO / libellé), jamais un UUID
        // en dur : la table pays/corps_de_metier est réinsérée par la migration V1 avec un id
        // généré à chaque nouvelle base (voir V1__init_schema.sql), donc un UUID figé ici casserait
        // dès qu'on tourne sur une base fraîche. null si la ligne de référence est absente (ne
        // bloque pas le seed, juste moins de champs renseignés).
        UUID franceId = paysRepository.findByCodeIso("FR").map(p -> p.getId()).orElse(null);
        UUID maconId = corpsDeMetierRepository.findByLibelle("MACON").map(c -> c.getId()).orElse(null);
        Entreprise entreprise = entrepriseRepository.save(Entreprise.creer(
            "Entreprise Test", "45289100300021", "12 rue de la Paix", null, null, "75002", "Paris",
            franceId, maconId, "01 23 45 67 89", null, null, null,
            "contact@entreprise-test.fr", null, null, "SARL",
            "452891003", "RCS Paris B 452 891 003", "FR62452891003", "1234567890",
            "M. Test RESPONSABLE", "Entreprise de test — créée automatiquement au démarrage."));
        creerCompte("entreprise.test", "Test", "Entreprise", "entreprise.test@admincontrol.local",
            Set.of("ENTREPRISE"), entreprise.getId(), null, null);
        log.warn("Compte de test créé : username=entreprise.test password={} (rôle ENTREPRISE, entrepriseId={})",
            PASSWORD, entreprise.getId());
    }

    private void seedClient() {
        if (utilisateurRepository.existsByUsername("client.test")) {
            return;
        }
        UUID franceId = paysRepository.findByCodeIso("FR").map(p -> p.getId()).orElse(null);
        Client client = clientRepository.save(Client.creer(
            "Client Test", "28 avenue Maréchal Foch", null, null, "69006", "Lyon", franceId,
            "04 72 18 45 30", null, null, null,
            "contact@client-test.fr", null, null, "SAS",
            "521774902", "52177490200034", "RCS Lyon B 521 774 902", "FR18521774902",
            "9876543210", "Mme Test RESPONSABLE"));
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
