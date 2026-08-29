package com.fluttiris.admincontrol.auth;

import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateur;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateurRepository;
import com.fluttiris.admincontrol.chantier.domain.RecurrenceControles;
import com.fluttiris.admincontrol.client.domain.Client;
import com.fluttiris.admincontrol.client.domain.ClientRepository;
import com.fluttiris.admincontrol.configuration.domain.CorpsDeMetierRepository;
import com.fluttiris.admincontrol.configuration.domain.PaysRepository;
import com.fluttiris.admincontrol.controle.domain.Controle;
import com.fluttiris.admincontrol.controle.domain.ControleRepository;
import com.fluttiris.admincontrol.controle.domain.RapportControle;
import com.fluttiris.admincontrol.controle.domain.RapportControleRepository;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.Message;
import com.fluttiris.admincontrol.messagerie.domain.MessageRepository;
import com.fluttiris.admincontrol.configuration.domain.SalarieFonction;
import com.fluttiris.admincontrol.configuration.domain.SalarieFonctionRepository;
import com.fluttiris.admincontrol.configuration.domain.TypeContratSalarie;
import com.fluttiris.admincontrol.configuration.domain.TypeContratSalarieRepository;
import com.fluttiris.admincontrol.configuration.domain.TypeSalarie;
import com.fluttiris.admincontrol.configuration.domain.TypeSalarieRepository;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierRepository;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import com.fluttiris.admincontrol.entreprise.domain.RoleEntreprise;
import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantier;
import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantierRepository;
import com.fluttiris.admincontrol.salarie.domain.Salarie;
import com.fluttiris.admincontrol.salarie.domain.SalarieRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Jeu de données de démonstration complet, créé au démarrage si absent (même idiome
 * d'idempotence que {@link TestAccountsSeeder} : une seule vérification par username
 * en tête de {@link #run}, jamais de re-création à chaque redémarrage). Séparé de
 * TestAccountsSeeder — qui reste le point d'entrée "un compte par rôle" utilisé pour les
 * vérifications manuelles rapides — parce que ce jeu-ci est volontairement volumineux :
 *
 * <ul>
 *   <li>5 Entreprises (un corps de métier BTP différent chacune), 3 comptes ENTREPRISE et
 *       10 Salariés par entreprise (50 au total) ;</li>
 *   <li>5 Clients, 5 comptes CLIENT par client (25 au total) — le premier de chaque client
 *       a accesTousChantiers=true (voir Utilisateur), les 4 autres sont scopés chantier par
 *       chantier via ChantierUtilisateur (modèle opt-in) ;</li>
 *   <li>7 Chantiers répartis entre les 5 clients ;</li>
 *   <li>Affectations Entreprise↔Chantier (dont une hiérarchie PRINCIPALE/STT1 pour illustrer
 *       la sous-traitance) et Salarié↔Chantier (accès accordé) pour que le jeu de données
 *       soit réellement navigable de bout en bout, pas seulement des lignes isolées.</li>
 * </ul>
 *
 * Tous les comptes utilisateur créés ici ont pour mot de passe {@code test1234}.
 *
 * Complété (demande explicite : rien de tout ça n'était testable jusqu'ici avec des données
 * réelles) avec un niveau STT2 dans la hiérarchie de sous-traitance, des Contrôles + Rapports
 * de contrôle rattachés au compte {@code controleur.test}, et quelques Messages — voir
 * {@link #seedControlesEtRapports} / {@link #seedMessages}. @Order(2) : tourne APRÈS
 * TestAccountsSeeder (@Order(1)), dont les comptes admin.local/controleur.test doivent déjà
 * exister pour être réutilisés ici.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String PASSWORD = "test1234";

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ClientRepository clientRepository;
    private final ChantierRepository chantierRepository;
    private final SalarieRepository salarieRepository;
    private final AffectationEntrepriseChantierRepository affectationEntrepriseChantierRepository;
    private final AffectationSalarieChantierRepository affectationSalarieChantierRepository;
    private final ChantierUtilisateurRepository chantierUtilisateurRepository;
    private final PaysRepository paysRepository;
    private final CorpsDeMetierRepository corpsDeMetierRepository;
    private final TypeSalarieRepository typeSalarieRepository;
    private final TypeContratSalarieRepository typeContratSalarieRepository;
    private final SalarieFonctionRepository salarieFonctionRepository;
    private final ControleRepository controleRepository;
    private final RapportControleRepository rapportControleRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    /** Une entreprise BTP = un corps de métier réel (voir V1__init_schema.sql) + un pool de
        fonctions salarié réelles cohérentes avec ce métier (voir V1, table salarie_fonction). */
    private record ProfilEntreprise(String raisonSociale, String siren, String ville, String codePostal,
                                     String telephone, String corpsDeMetierLibelle, String[] fonctions) {
    }

    // siren : 9 chiffres exactement (numéro fictif mais correctement dimensionné, pas de valeur réelle).
    private static final ProfilEntreprise[] PROFILS_ENTREPRISE = {
        new ProfilEntreprise("BTP Construction Rhône", "489123008", "Lyon", "69003", "04 78 42 15 30",
            "MACON", new String[]{"MACON", "AIDE MACON", "MACON COFFREUR", "MACON - VRD", "MACON PAYSAGISTE",
                "CHEF D'EQUIPE", "CONDUCTEUR DE TRAVAUX", "MANOEUVRE", "CHAUFFEUR", "GRUTIER"}),
        new ProfilEntreprise("Menuiserie Dupont & Fils", "502345001", "Villeurbanne", "69100", "04 78 68 22 11",
            "MENUISERIE", new String[]{"MENUISIER", "AIDE MENUISIER", "AIDE MENUISIER ALUMINIUM",
                "CHEF D'EQUIPE MENUISIER ALUMINIUM", "CONDUCTEUR DE TRAVAUX", "CHEF D'EQUIPE", "MANOEUVRE",
                "CHAUFFEUR", "AIDE CONDUCTEUR DE TRAVAUX", "GRUTIER"}),
        new ProfilEntreprise("Électricité Générale Moreau", "513456007", "Vénissieux", "69200", "04 78 76 33 44",
            "ELECTRICITE", new String[]{"ELECTRICIEN", "AIDE ELECTRICIEN", "APPRENTI ELECTRICIEN",
                "CHEF D'EQUIPE ELECTRICIEN", "CHEF ELECTRICIEN", "CONDUCTEUR DE TRAVAUX", "CHEF D'EQUIPE",
                "MANOEUVRE", "CHAUFFEUR", "AIDE CONDUCTEUR DE TRAVAUX"}),
        new ProfilEntreprise("Plomberie Sanitaire Lefebvre", "524567003", "Caluire-et-Cuire", "69300", "04 78 23 55 66",
            "PLOMBIER", new String[]{"PLOMBIER", "AIDE PLOMBIER", "AIDE PLOMBIER CHAUFFAGISTE",
                "APPRENTI PLOMBIER", "CHEF D'EQUIPE PLOMBIERS", "CONDUCTEUR DE TRAVAUX", "CHEF D'EQUIPE",
                "MANOEUVRE", "CHAUFFEUR", "GRUTIER"}),
        new ProfilEntreprise("Terrassement Bernard SARL", "535678009", "Bron", "69500", "04 78 54 77 88",
            "DEMOLITION - TERRASSEMENT", new String[]{"MANOEUVRE", "CHEF DE MANOEUVRE", "CONDUCTEUR DE TRAVAUX",
                "CHEF D'EQUIPE", "CHAUFFEUR", "GRUTIER", "MACON - VRD", "AIDE CONDUCTEUR DE TRAVAUX",
                "MACON", "MACON COFFREUR"})
    };

    private record ProfilClient(String raisonSociale, String ville, String codePostal, String telephone,
                                 String formeJuridique) {
    }

    private static final ProfilClient[] PROFILS_CLIENT = {
        new ProfilClient("Mairie de Lyon", "Lyon", "69002", "04 72 10 30 30", "Collectivité"),
        new ProfilClient("Conseil Départemental du Rhône", "Lyon", "69003", "04 72 61 79 79", "Collectivité"),
        new ProfilClient("SNCF Réseau", "Lyon", "69009", "04 72 84 26 00", "SA"),
        new ProfilClient("Groupe Immobilier Vinci", "Lyon", "69006", "04 72 83 44 00", "SAS"),
        new ProfilClient("Université Claude Bernard Lyon 1", "Villeurbanne", "69622", "04 72 44 80 00", "Établissement public")
    };

    /** nom du chantier, index du client (dans PROFILS_CLIENT), prestation, décalage (jours) dateDebut/dateFin. */
    private record ProfilChantier(String nom, int clientIndex, String prestation, long joursDebut, long joursFin,
                                   RecurrenceControles recurrence) {
    }

    private static final ProfilChantier[] PROFILS_CHANTIER = {
        new ProfilChantier("Réhabilitation École Jean Jaurès", 0, "Rénovation énergétique", -120, 90, RecurrenceControles.MENSUEL),
        new ProfilChantier("Rénovation Hôtel de Ville", 0, "Ravalement de façade", -60, 150, RecurrenceControles.TRIMESTRIEL),
        new ProfilChantier("Réfection RD383", 1, "Voirie et réseaux divers", -30, 180, RecurrenceControles.MENSUEL),
        new ProfilChantier("Modernisation Gare Part-Dieu", 2, "Second œuvre", -200, 300, RecurrenceControles.MENSUEL),
        new ProfilChantier("Résidence Les Terrasses du Parc", 3, "Construction neuve", -90, 400, RecurrenceControles.TRIMESTRIEL),
        new ProfilChantier("Extension Campus LyonTech", 4, "Extension de bâtiment", -45, 250, RecurrenceControles.MENSUEL),
        new ProfilChantier("Parking Silo Universitaire", 4, "Construction neuve", -15, 220, RecurrenceControles.TRIMESTRIEL)
    };

    /** entreprise principale (index PROFILS_ENTREPRISE) par chantier (index PROFILS_CHANTIER). */
    private static final int[] ENTREPRISE_PRINCIPALE_PAR_CHANTIER = {0, 1, 2, 0, 3, 4, 4};

    private static final String[] PRENOMS_H = {"Jean", "Pierre", "Michel", "Alain", "Philippe", "Daniel", "Jacques", "Nicolas", "Julien", "Thomas"};
    private static final String[] PRENOMS_F = {"Marie", "Nathalie", "Isabelle", "Sylvie", "Catherine", "Christine", "Sophie", "Sandrine", "Aurélie", "Camille"};
    private static final String[] NOMS = {"Martin", "Bernard", "Dubois", "Thomas", "Robert", "Petit", "Durand", "Leroy",
        "Moreau", "Simon", "Laurent", "Lefebvre", "Roux", "Vincent", "Fontaine", "Chevalier", "Garnier", "Faure", "Rousseau", "Blanc"};

    @Override
    public void run(ApplicationArguments args) {
        // Un seul point d'idempotence pour tout le lot : Entreprise/Client/Chantier/Salarie
        // n'ont pas de clé métier unique exploitable (voir EntrepriseRepository/ClientRepository,
        // pas de findByRaisonSociale) — plutôt que d'ajouter une vérification par entité, tout le
        // jeu de données est créé comme un seul bloc atomique, gardé par le premier compte créé.
        if (utilisateurRepository.existsByUsername("entreprise1.responsable")) {
            return;
        }

        UUID franceId = paysRepository.findByCodeIso("FR").map(p -> p.getId()).orElse(null);
        UUID typeSalarieId = codeVersId(typeSalarieRepository.findAll(), TypeSalarie::getCode, TypeSalarie::getId, "SALARIE");
        UUID typeCdiId = codeVersId(typeContratSalarieRepository.findAll(), TypeContratSalarie::getCode, TypeContratSalarie::getId, "CDI");
        UUID typeCddId = codeVersId(typeContratSalarieRepository.findAll(), TypeContratSalarie::getCode, TypeContratSalarie::getId, "CDD");
        Map<String, UUID> fonctionIdParLibelle = salarieFonctionRepository.findAll().stream()
            .collect(Collectors.toMap(SalarieFonction::getLibelle, SalarieFonction::getId, (a, b) -> a));

        List<Entreprise> entreprises = seedEntreprises(franceId);
        List<Client> clients = seedClients(franceId);
        List<Chantier> chantiers = seedChantiers(clients);
        seedUtilisateursEntreprise(entreprises);
        Map<Integer, List<Chantier>> chantiersParClient = seedUtilisateursClient(clients, chantiers);
        List<List<Salarie>> salariesParEntreprise = seedSalaries(entreprises, franceId, typeSalarieId, typeCdiId, typeCddId, fonctionIdParLibelle);
        List<AffectationEntrepriseChantier> affectationsPrincipales = seedAffectationsEntrepriseChantier(entreprises, chantiers);
        seedAffectationsSalarieChantier(chantiers, salariesParEntreprise, affectationsPrincipales);

        // Réutilisent les comptes créés par TestAccountsSeeder (voir @Order) — se connecter avec
        // controleur.test/admin.local montre directement des données réelles, pas des listes vides.
        UUID adminId = utilisateurRepository.findByUsername("admin.local").map(Utilisateur::getId).orElse(null);
        utilisateurRepository.findByUsername("controleur.test").ifPresentOrElse(
            controleur -> seedControlesEtRapports(chantiers, controleur.getId(), controleur.getControleTiersId(), adminId),
            () -> log.warn("Compte controleur.test introuvable — contrôles/rapports de démo non créés."));
        seedMessages(chantiers, entreprises, clients, salariesParEntreprise, adminId);

        log.warn("Jeu de données de démo créé : {} entreprises ({} comptes ENTREPRISE, {} salariés), "
                + "{} clients ({} comptes CLIENT), {} chantiers, hiérarchie PRINCIPALE/STT1/STT2, "
                + "contrôles/rapports, messages. Mot de passe de tous les comptes créés : {}",
            entreprises.size(), entreprises.size() * 3, entreprises.size() * 10,
            clients.size(), clients.size() * 5, chantiers.size(), PASSWORD);
    }

    private <T> UUID codeVersId(List<T> items, Function<T, String> code, Function<T, UUID> id, String recherche) {
        return items.stream().filter(i -> recherche.equals(code.apply(i))).findFirst().map(id).orElse(null);
    }

    private List<Entreprise> seedEntreprises(UUID franceId) {
        List<Entreprise> resultat = new ArrayList<>();
        for (int i = 0; i < PROFILS_ENTREPRISE.length; i++) {
            ProfilEntreprise p = PROFILS_ENTREPRISE[i];
            UUID corpsDeMetierId = corpsDeMetierRepository.findByLibelle(p.corpsDeMetierLibelle()).map(c -> c.getId()).orElse(null);
            String siren = p.siren();
            String siret = siren + "00013";
            String slug = slug(p.raisonSociale());
            Entreprise entreprise = Entreprise.creer(
                p.raisonSociale(), siret, (10 + i) + " rue de l'Industrie", null, null, p.codePostal(), p.ville(),
                franceId, corpsDeMetierId, p.telephone(), null, null, null,
                "contact@" + slug + ".fr", null, null, "SARL",
                siren, "RCS Lyon B " + formaterSiren(siren), "FR" + (10 + i) + siren, "1" + (100000000 + i * 111111),
                "M. Responsable " + p.raisonSociale().split(" ")[0], "Entreprise de démonstration créée automatiquement.");
            resultat.add(entrepriseRepository.save(entreprise));
        }
        return resultat;
    }

    private List<Client> seedClients(UUID franceId) {
        List<Client> resultat = new ArrayList<>();
        for (int i = 0; i < PROFILS_CLIENT.length; i++) {
            ProfilClient p = PROFILS_CLIENT[i];
            String siren = String.format("6012345%02d", i); // 9 chiffres exactement
            String slug = slug(p.raisonSociale());
            Client client = Client.creer(
                p.raisonSociale(), (1 + i) + " place de la République", null, null, p.codePostal(), p.ville(),
                franceId, p.telephone(), null, null, null,
                "contact@" + slug + ".fr", null, null, p.formeJuridique(),
                siren, siren + "00019", "RCS Lyon B " + formaterSiren(siren), "FR" + (20 + i) + siren,
                "2" + (200000000 + i * 111111), "Mme Responsable " + p.raisonSociale().split(" ")[0]);
            resultat.add(clientRepository.save(client));
        }
        return resultat;
    }

    private List<Chantier> seedChantiers(List<Client> clients) {
        List<Chantier> resultat = new ArrayList<>();
        LocalDate aujourdHui = LocalDate.now();
        for (ProfilChantier p : PROFILS_CHANTIER) {
            Client client = clients.get(p.clientIndex());
            Chantier chantier = Chantier.creer(
                p.nom(), client.getId(), p.prestation(), "Chantier " + p.nom(), null, null,
                client.getCodePostal(), client.getVille(), client.getPaysId(),
                "Chantier de démonstration.", "Chantier suivi dans le cadre du contrôle de conformité.",
                null, aujourdHui.plusDays(p.joursDebut()), aujourdHui.plusDays(p.joursFin()));
            chantier.definirControles(p.recurrence(), aujourdHui.plusMonths(1));
            resultat.add(chantierRepository.save(chantier));
        }
        return resultat;
    }

    private void seedUtilisateursEntreprise(List<Entreprise> entreprises) {
        String[] fonctionsCompte = {"Responsable", "Assistant", "Conducteur de travaux"};
        for (int e = 0; e < entreprises.size(); e++) {
            Entreprise entreprise = entreprises.get(e);
            for (int u = 0; u < 3; u++) {
                String prenom = prenom(e * 3 + u);
                String nom = NOMS[(e * 3 + u) % NOMS.length];
                String username = "entreprise" + (e + 1) + "." + (u == 0 ? "responsable" : "user" + (u + 1));
                creerCompte(username, nom, prenom + " (" + fonctionsCompte[u] + ")",
                    slug(prenom) + "." + slug(nom) + "@" + slug(entreprise.getRaisonSociale()) + ".fr",
                    Set.of("ENTREPRISE"), entreprise.getId(), null, null, false);
            }
        }
    }

    /** @return les chantiers du client, regroupés par index client (dans PROFILS_CLIENT), pour l'affectation des salariés plus bas. */
    private Map<Integer, List<Chantier>> seedUtilisateursClient(List<Client> clients, List<Chantier> chantiers) {
        Map<Integer, List<Chantier>> chantiersParClient = new java.util.HashMap<>();
        for (int c = 0; c < PROFILS_CHANTIER.length; c++) {
            chantiersParClient.computeIfAbsent(PROFILS_CHANTIER[c].clientIndex(), k -> new ArrayList<>()).add(chantiers.get(c));
        }
        for (int c = 0; c < clients.size(); c++) {
            Client client = clients.get(c);
            List<Chantier> chantiersDuClient = chantiersParClient.getOrDefault(c, List.of());
            for (int u = 0; u < 5; u++) {
                String prenom = prenom(100 + c * 5 + u);
                String nom = NOMS[(10 + c * 5 + u) % NOMS.length];
                String username = "client" + (c + 1) + "." + (u == 0 ? "responsable" : "user" + (u + 1));
                // Le premier compte de chaque client a accès à tous ses chantiers d'office ; les
                // 4 autres illustrent le modèle opt-in (ChantierUtilisateur), un par chantier du
                // client à tour de rôle — pas d'accès du tout si le client n'a aucun chantier.
                boolean accesTousChantiers = u == 0;
                Utilisateur utilisateur = creerCompte(username, nom, prenom,
                    slug(prenom) + "." + slug(nom) + "@" + slug(client.getRaisonSociale()) + ".fr",
                    Set.of("CLIENT"), null, client.getId(), null, accesTousChantiers);
                if (!accesTousChantiers && !chantiersDuClient.isEmpty()) {
                    Chantier chantierAssigne = chantiersDuClient.get((u - 1) % chantiersDuClient.size());
                    chantierUtilisateurRepository.save(ChantierUtilisateur.creer(chantierAssigne.getId(), utilisateur.getId()));
                }
            }
        }
        return chantiersParClient;
    }

    private List<List<Salarie>> seedSalaries(List<Entreprise> entreprises, UUID franceId, UUID typeSalarieId,
                                              UUID typeCdiId, UUID typeCddId, Map<String, UUID> fonctionIdParLibelle) {
        List<List<Salarie>> resultat = new ArrayList<>();
        for (int e = 0; e < entreprises.size(); e++) {
            Entreprise entreprise = entreprises.get(e);
            ProfilEntreprise profil = PROFILS_ENTREPRISE[e];
            List<Salarie> salariesEntreprise = new ArrayList<>();
            for (int s = 0; s < 10; s++) {
                int index = e * 10 + s;
                String prenom = prenom(index);
                String nom = NOMS[index % NOMS.length];
                LocalDate naissance = LocalDate.of(1965 + (index % 38), 1 + (index % 12), 1 + (index % 28));
                UUID typeContratId = s % 4 == 0 ? typeCddId : typeCdiId; // 1 salarié sur 4 en CDD, le reste en CDI
                UUID fonctionId = fonctionIdParLibelle.get(profil.fonctions()[s % profil.fonctions().length]);
                Salarie salarie = Salarie.creer(nom, prenom, naissance, franceId, entreprise.getId(),
                    typeSalarieId, typeContratId, fonctionId);
                salariesEntreprise.add(salarieRepository.save(salarie));
            }
            resultat.add(salariesEntreprise);
        }
        return resultat;
    }

    private List<AffectationEntrepriseChantier> seedAffectationsEntrepriseChantier(List<Entreprise> entreprises, List<Chantier> chantiers) {
        List<AffectationEntrepriseChantier> principales = new ArrayList<>();
        for (int c = 0; c < chantiers.size(); c++) {
            Entreprise principale = entreprises.get(ENTREPRISE_PRINCIPALE_PAR_CHANTIER[c]);
            AffectationEntrepriseChantier affectation = AffectationEntrepriseChantier.creer(
                chantiers.get(c).getId(), principale.getId(), RoleEntreprise.PRINCIPALE, null);
            principales.add(affectationEntrepriseChantierRepository.save(affectation));
        }
        // Illustre la sous-traitance à 2 niveaux (demande explicite : STT2 jamais présent jusqu'ici,
        // impossible à tester) : sur le chantier 0, Électricité Générale Moreau (index 2) intervient
        // comme STT1 sous la principale BTP Construction Rhône, et Plomberie Sanitaire Lefebvre
        // (index 3) intervient comme STT2 sous CE STT1 — une vraie hiérarchie à 3 niveaux.
        AffectationEntrepriseChantier principaleChantier0 = principales.get(0);
        AffectationEntrepriseChantier stt1Chantier0 = affectationEntrepriseChantierRepository.save(AffectationEntrepriseChantier.creer(
            chantiers.get(0).getId(), entreprises.get(2).getId(), RoleEntreprise.STT1, principaleChantier0));
        affectationEntrepriseChantierRepository.save(AffectationEntrepriseChantier.creer(
            chantiers.get(0).getId(), entreprises.get(3).getId(), RoleEntreprise.STT2, stt1Chantier0));
        return principales;
    }

    private void seedAffectationsSalarieChantier(List<Chantier> chantiers, List<List<Salarie>> salariesParEntreprise,
                                                  List<AffectationEntrepriseChantier> affectationsPrincipales) {
        for (int c = 0; c < chantiers.size(); c++) {
            int entrepriseIndex = ENTREPRISE_PRINCIPALE_PAR_CHANTIER[c];
            List<Salarie> salaries = salariesParEntreprise.get(entrepriseIndex);
            UUID affectationEntrepriseChantierId = affectationsPrincipales.get(c).getId();
            // 3 premiers salariés de l'entreprise principale, accès accordé, pour un jeu de
            // données déjà "prêt à contrôler" plutôt que des chantiers sans personne dessus.
            for (int s = 0; s < 3; s++) {
                Salarie salarie = salaries.get(s);
                if (affectationSalarieChantierRepository.existsBySalarieIdAndChantierIdAndDateFinIsNull(salarie.getId(), chantiers.get(c).getId())) {
                    continue;
                }
                AffectationSalarieChantier affectation = AffectationSalarieChantier.creer(
                    salarie.getId(), chantiers.get(c).getId(), affectationEntrepriseChantierId);
                affectation.accorderAcces();
                affectationSalarieChantierRepository.save(affectation);
            }
        }
    }

    /** Contrôles + rapports rattachés au compte controleur.test (voir run(), @Order) — pour
        chaque chantier : un ancien contrôle toujours terminé + rapporté (historique réel à
        consulter), puis un contrôle récent dont l'issue tourne selon le chantier pour couvrir
        les 3 états affichés côté front (Terminé / En retard / Programmé — voir
        ChantierListComponent.prochainControleEnRetard et statutControle). */
    private void seedControlesEtRapports(List<Chantier> chantiers, UUID controleurUtilisateurId,
                                          UUID controleTiersId, UUID responsableUtilisateurId) {
        LocalDate aujourdHui = LocalDate.now();
        for (int c = 0; c < chantiers.size(); c++) {
            UUID chantierId = chantiers.get(c).getId();

            Controle ancien = controleRepository.save(Controle.creer(
                chantierId, controleurUtilisateurId, aujourdHui.minusMonths(4).plusDays(c),
                "Contrôle de conformité initial — RAS.", controleTiersId, aujourdHui.minusMonths(4).plusDays(c), true));
            rapportControleRepository.save(RapportControle.creer(
                ancien.getId(), 3, 3, 0, 0, 0, 1, 0, responsableUtilisateurId));

            switch (c % 3) {
                case 0 -> {
                    Controle termine = controleRepository.save(Controle.creer(
                        chantierId, controleurUtilisateurId, aujourdHui.minusDays(10 + c),
                        "Contrôle périodique — situation conforme.", controleTiersId, aujourdHui.minusDays(10 + c), true));
                    rapportControleRepository.save(RapportControle.creer(
                        termine.getId(), 4, 4, 0, 1, 1, 2, 0, responsableUtilisateurId));
                }
                case 1 -> controleRepository.save(Controle.creer(
                    chantierId, controleurUtilisateurId, aujourdHui.minusDays(5 + c),
                    "Contrôle périodique à finaliser.", controleTiersId, null, false));
                default -> controleRepository.save(Controle.creer(
                    chantierId, controleurUtilisateurId, aujourdHui.plusDays(15 + c),
                    null, controleTiersId, null, false));
            }
        }
    }

    /** Quelques messages de démo (demande explicite : aucun jusqu'ici, historique des messages
        toujours vide) — envoyés par admin.local vers une Entreprise, un Client et un Salarié
        précis, pour couvrir les 3 contextes où "Historique des messages" est affiché dans
        l'app (fiches Entreprise/Client/Salarié). Rien créé si admin.local est introuvable. */
    private void seedMessages(List<Chantier> chantiers, List<Entreprise> entreprises, List<Client> clients,
                               List<List<Salarie>> salariesParEntreprise, UUID adminId) {
        if (adminId == null) {
            log.warn("Compte admin.local introuvable — messages de démo non créés.");
            return;
        }
        UUID chantier0Id = chantiers.get(0).getId();
        Salarie premierSalarie = salariesParEntreprise.get(0).get(0);

        messageRepository.save(Message.envoyer(adminId, chantier0Id, DestinataireType.ENTREPRISE, entreprises.get(0).getId(),
            "Document à fournir — attestation de vigilance URSSAF",
            "<p>Bonjour,</p><p>Merci de nous transmettre l'attestation de vigilance URSSAF à jour pour le chantier "
                + chantiers.get(0).getNom() + ".</p><p>Cordialement.</p>"));

        messageRepository.save(Message.envoyer(adminId, chantier0Id, DestinataireType.CLIENT, clients.get(0).getId(),
            "Point d'avancement — " + chantiers.get(0).getNom(),
            "<p>Bonjour,</p><p>Le contrôle de conformité mensuel a été réalisé, aucune anomalie relevée.</p><p>Cordialement.</p>"));

        messageRepository.save(Message.envoyer(adminId, chantier0Id, DestinataireType.ENTREPRISE, entreprises.get(0).getId(),
            "Document à fournir — carte BTP de " + premierSalarie.getPrenom() + " " + premierSalarie.getNom(),
            "<p>Bonjour,</p><p>Merci de nous transmettre la carte BTP de " + premierSalarie.getPrenom() + " "
                + premierSalarie.getNom() + ".</p><p>Cordialement.</p>",
            null, premierSalarie.getId()));

        messageRepository.save(Message.envoyer(adminId, chantiers.get(4).getId(), DestinataireType.ENTREPRISE, entreprises.get(3).getId(),
            "Rappel — document en attente",
            "<p>Bonjour,</p><p>Dernier rappel : un document obligatoire reste manquant sur ce chantier.</p><p>Cordialement.</p>"));
    }

    private Utilisateur creerCompte(String username, String nom, String prenom, String email, Set<String> roles,
                                     UUID entrepriseId, UUID clientId, UUID controleTiersId, boolean accesTousChantiers) {
        Utilisateur utilisateur = Utilisateur.creer(username, passwordEncoder.encode(PASSWORD), null, nom, prenom,
            email, roles, entrepriseId, clientId, controleTiersId, accesTousChantiers);
        return utilisateurRepository.save(utilisateur);
    }

    private String prenom(int index) {
        return index % 2 == 0 ? PRENOMS_H[index % PRENOMS_H.length] : PRENOMS_F[index % PRENOMS_F.length];
    }

    private String slug(String texte) {
        return texte.toLowerCase()
            .replaceAll("[éèêë]", "e").replaceAll("[àâ]", "a").replaceAll("[ôö]", "o").replaceAll("[ùû]", "u")
            .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String formaterSiren(String siren) {
        return siren.substring(0, 3) + " " + siren.substring(3, 6) + " " + siren.substring(6, 9);
    }
}
