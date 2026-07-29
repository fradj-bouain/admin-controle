package com.fluttiris.admincontrol.messagerie.application;

import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.client.domain.Client;
import com.fluttiris.admincontrol.client.domain.ClientRepository;
import com.fluttiris.admincontrol.document.domain.Document;
import com.fluttiris.admincontrol.document.domain.DocumentRepository;
import com.fluttiris.admincontrol.document.domain.TypeDocument;
import com.fluttiris.admincontrol.document.domain.TypeDocumentRepository;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifieRepository;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisation;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisationRepository;
import com.fluttiris.admincontrol.messagerie.domain.StatutMessagePlanifie;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Moteur d'automatisation de la messagerie : génère des messages planifiés à
 * partir des règles actives (relance N jours avant un événement métier), puis
 * envoie tout message planifié (manuel ou généré) arrivé à échéance.
 */
@Service
@RequiredArgsConstructor
public class AutomatisationSchedulerService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RegleAutomatisationRepository regleAutomatisationRepository;
    private final MessagePlanifieRepository messagePlanifieRepository;
    private final DocumentRepository documentRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final ChantierRepository chantierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ClientRepository clientRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final MessageService messageService;

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void genererMessagesDepuisRegles() {
        for (RegleAutomatisation regle : regleAutomatisationRepository.findByActifTrue()) {
            switch (regle.getEvenementDeclencheur()) {
                case DOCUMENT_EXPIRATION -> genererPourDocuments(regle);
                case CHANTIER_CONTROLE_A_VENIR -> genererPourChantiers(regle);
            }
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void envoyerMessagesDus() {
        List<MessagePlanifie> dus = messagePlanifieRepository
            .findByStatutAndDateEnvoiPrevueLessThanEqualOrderByDateEnvoiPrevue(StatutMessagePlanifie.EN_ATTENTE, Instant.now());
        for (MessagePlanifie message : dus) {
            for (UUID destinataireId : resoudreDestinataires(message)) {
                messageService.envoyer(message.getExpediteurUtilisateurId(), message.getChantierId(),
                    resoudreDestinataireType(message), destinataireId, message.getSujet(), message.getContenu());
            }
            message.marquerEnvoye();
        }
    }

    private void genererPourDocuments(RegleAutomatisation regle) {
        LocalDate dateCible = LocalDate.now().plusDays(regle.getNbJoursAvant());
        for (Document document : documentRepository.findByDateExpiration(dateCible)) {
            if (messagePlanifieRepository.existsByRegleIdAndSourceEntityId(regle.getId(), document.getId())) {
                continue;
            }
            String libelle = typeDocumentRepository.findById(document.getTypeDocumentId())
                .map(TypeDocument::getLibelle).orElse("");
            String sujet = substituer(regle.getSujet(), document.getDateExpiration(), null, libelle);
            String contenu = substituer(regle.getContenu(), document.getDateExpiration(), null, libelle);
            messagePlanifieRepository.save(MessagePlanifie.genererDepuisRegle(regle, document.getId(),
                document.getChantierId(), sujet, contenu, Instant.now()));
        }
    }

    private void genererPourChantiers(RegleAutomatisation regle) {
        LocalDate dateCible = LocalDate.now().plusDays(regle.getNbJoursAvant());
        for (Chantier chantier : chantierRepository.findByDateProchainControle(dateCible)) {
            if (messagePlanifieRepository.existsByRegleIdAndSourceEntityId(regle.getId(), chantier.getId())) {
                continue;
            }
            String sujet = substituer(regle.getSujet(), chantier.getDateProchainControle(), chantier.getNom(), null);
            String contenu = substituer(regle.getContenu(), chantier.getDateProchainControle(), chantier.getNom(), null);
            messagePlanifieRepository.save(MessagePlanifie.genererDepuisRegle(regle, chantier.getId(),
                chantier.getId(), sujet, contenu, Instant.now()));
        }
    }

    private String substituer(String texte, LocalDate date, String chantierNom, String documentLibelle) {
        String resultat = texte;
        if (date != null) {
            resultat = resultat.replace("[DATE]", date.format(FORMAT_DATE));
        }
        if (chantierNom != null) {
            resultat = resultat.replace("[CHANTIER_NOM]", chantierNom);
        }
        if (documentLibelle != null) {
            resultat = resultat.replace("[DOCUMENT_LIBELLE]", documentLibelle);
        }
        return resultat;
    }

    private DestinataireType resoudreDestinataireType(MessagePlanifie message) {
        return switch (message.getCibleGroupe()) {
            case SPECIFIQUE -> message.getDestinataireType();
            case TOUS_UTILISATEURS -> DestinataireType.UTILISATEUR;
            case TOUS_CLIENTS -> DestinataireType.CLIENT;
            case TOUTES_ENTREPRISES -> DestinataireType.ENTREPRISE;
        };
    }

    private List<UUID> resoudreDestinataires(MessagePlanifie message) {
        return switch (message.getCibleGroupe()) {
            case SPECIFIQUE -> List.of(message.getDestinataireId());
            case TOUS_UTILISATEURS -> utilisateurRepository.findByActifTrue().stream().map(Utilisateur::getId).toList();
            case TOUS_CLIENTS -> clientRepository.findByActifTrue().stream().map(Client::getId).toList();
            case TOUTES_ENTREPRISES -> entrepriseRepository.findByActifTrue().stream().map(Entreprise::getId).toList();
        };
    }
}
