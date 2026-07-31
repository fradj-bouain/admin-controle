package com.fluttiris.admincontrol.messagerie.application;

import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.client.domain.Client;
import com.fluttiris.admincontrol.client.domain.ClientRepository;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.EvenementDetecte;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifieRepository;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisation;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisationRepository;
import com.fluttiris.admincontrol.messagerie.domain.StatutMessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.TypeDeclencheur;
import com.fluttiris.admincontrol.salarie.domain.Salarie;
import com.fluttiris.admincontrol.salarie.domain.SalarieRepository;
import com.fluttiris.admincontrol.salarie.domain.StatutSalarie;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Moteur d'automatisation de la messagerie. Trois sources génèrent des
 * MessagePlanifie : le scan quotidien des règles CHAMP_SURVEILLABLE (relance
 * N jours avant une échéance de date), le tick PERIODIQUE (récurrence pure,
 * indépendante de toute entité), et les événements de création/affectation
 * (voir AutomatisationEvenementListener, déclenché en temps réel). Un seul
 * tick envoie ensuite tout MessagePlanifie (manuel ou généré) arrivé à échéance.
 */
@Service
@RequiredArgsConstructor
public class AutomatisationSchedulerService {

    private final RegleAutomatisationRepository regleAutomatisationRepository;
    private final MessagePlanifieRepository messagePlanifieRepository;
    private final ChampSurveillableRegistry champSurveillableRegistry;
    private final UtilisateurRepository utilisateurRepository;
    private final ClientRepository clientRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final SalarieRepository salarieRepository;
    private final MessageService messageService;

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void genererMessagesDepuisRegles() {
        for (RegleAutomatisation regle : regleAutomatisationRepository.findByActifTrue()) {
            if (regle.getTypeDeclencheur() != TypeDeclencheur.CHAMP_SURVEILLABLE) {
                continue;
            }
            champSurveillableRegistry.parId(regle.getChampSurveillableId())
                .ifPresent(champ -> genererDepuisChampSurveillable(regle, champ.rechercher(LocalDate.now().plusDays(regle.getNbJoursAvant()))));
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void genererDepuisPeriodique() {
        LocalDate aujourdHui = LocalDate.now();
        for (RegleAutomatisation regle : regleAutomatisationRepository.findByActifTrue()) {
            if (regle.getTypeDeclencheur() != TypeDeclencheur.PERIODIQUE || !regle.periodiqueDue(aujourdHui)) {
                continue;
            }
            messagePlanifieRepository.save(
                MessagePlanifie.genererDepuisRegle(regle, null, null, regle.getSujet(), regle.getContenu(), Instant.now()));
            regle.marquerExecutee();
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void envoyerMessagesDus() {
        List<MessagePlanifie> dus = messagePlanifieRepository
            .findByStatutAndDateEnvoiPrevueLessThanEqualOrderByDateEnvoiPrevue(StatutMessagePlanifie.EN_ATTENTE, Instant.now());
        for (MessagePlanifie message : dus) {
            envoyerUnMessagePlanifie(message);
        }
    }

    /** Réutilisé par le tick périodique ET le déclenchement manuel ("Envoyer maintenant"). */
    void envoyerUnMessagePlanifie(MessagePlanifie message) {
        for (UUID destinataireId : resoudreDestinataires(message)) {
            messageService.envoyer(message.getExpediteurUtilisateurId(), message.getChantierId(),
                resoudreDestinataireType(message), destinataireId, message.getSujet(), message.getContenu());
        }
        message.marquerEnvoye();
        if (message.getRegleId() != null) {
            regleAutomatisationRepository.findById(message.getRegleId()).ifPresent(RegleAutomatisation::marquerEnvoyee);
        }
    }

    private void genererDepuisChampSurveillable(RegleAutomatisation regle, List<EvenementDetecte> evenements) {
        for (EvenementDetecte evenement : evenements) {
            if (messagePlanifieRepository.existsByRegleIdAndSourceEntityId(regle.getId(), evenement.sourceEntityId())) {
                continue;
            }
            String sujet = substituer(regle.getSujet(), evenement.placeholders());
            String contenu = substituer(regle.getContenu(), evenement.placeholders());
            messagePlanifieRepository.save(MessagePlanifie.genererDepuisRegle(regle, evenement.sourceEntityId(),
                evenement.chantierId(), sujet, contenu, Instant.now()));
        }
    }

    private String substituer(String texte, Map<String, String> placeholders) {
        String resultat = texte;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resultat = resultat.replace("[" + entry.getKey() + "]", entry.getValue());
        }
        return resultat;
    }

    private DestinataireType resoudreDestinataireType(MessagePlanifie message) {
        return switch (message.getCibleGroupe()) {
            case SPECIFIQUE -> message.getDestinataireType();
            case TOUS_UTILISATEURS -> DestinataireType.UTILISATEUR;
            case TOUS_CLIENTS -> DestinataireType.CLIENT;
            case TOUTES_ENTREPRISES, TOUS_SALARIES -> DestinataireType.ENTREPRISE;
        };
    }

    private List<UUID> resoudreDestinataires(MessagePlanifie message) {
        return switch (message.getCibleGroupe()) {
            case SPECIFIQUE -> List.of(message.getDestinataireId());
            case TOUS_UTILISATEURS -> utilisateurRepository.findByActifTrue().stream().map(Utilisateur::getId).toList();
            case TOUS_CLIENTS -> clientRepository.findByActifTrue().stream().map(Client::getId).toList();
            case TOUTES_ENTREPRISES -> entrepriseRepository.findByActifTrue().stream().map(Entreprise::getId).toList();
            // Pas de compte/boîte de réception propre aux salariés : on route vers
            // l'ensemble (dédupliqué) des entreprises qui emploient un salarié actif.
            case TOUS_SALARIES -> salarieRepository.findByStatut(StatutSalarie.ACTIF).stream()
                .map(Salarie::getEntrepriseEmployeurId).distinct().toList();
        };
    }
}
