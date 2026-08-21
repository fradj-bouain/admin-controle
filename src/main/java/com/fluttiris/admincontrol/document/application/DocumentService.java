package com.fluttiris.admincontrol.document.application;

import com.fluttiris.admincontrol.common.audit.HistoriqueModificationResponse;
import com.fluttiris.admincontrol.common.audit.HistoriqueModificationService;
import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.common.mail.EmailService;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import com.fluttiris.admincontrol.document.api.dto.ConformitePortefeuilleResponse;
import com.fluttiris.admincontrol.document.api.dto.DocumentEnAttenteResponse;
import com.fluttiris.admincontrol.document.api.dto.DocumentExpirantResponse;
import com.fluttiris.admincontrol.document.domain.CibleDocument;
import com.fluttiris.admincontrol.document.domain.Document;
import com.fluttiris.admincontrol.document.domain.DocumentEtatRepository;
import com.fluttiris.admincontrol.document.domain.DocumentRepository;
import com.fluttiris.admincontrol.document.domain.StatutValidation;
import com.fluttiris.admincontrol.document.domain.TypeDocument;
import com.fluttiris.admincontrol.document.domain.TypeDocumentRepository;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import com.fluttiris.admincontrol.messagerie.api.dto.MessagePlanifieResponse;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifieRepository;
import com.fluttiris.admincontrol.salarie.domain.SalarieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private static final String ENTITE = "DOCUMENT";

    private final DocumentRepository documentRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final DocumentEtatRepository documentEtatRepository;
    private final SalarieRepository salarieRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final MessagePlanifieRepository messagePlanifieRepository;
    private final EmailService emailService;
    private final HistoriqueModificationService historiqueService;
    private final DocumentStorageService documentStorageService;
    private final CurrentUser currentUser;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public Document creer(UUID typeDocumentId, UUID salarieId, UUID entrepriseId, UUID chantierId,
                           String fichierUrl, LocalDate dateDebutValidite, LocalDate dateExpiration,
                           LocalDate dateRelance, String mentions, MultipartFile fichier) {
        DocumentStorageService.StoredFile stocke = fichier != null && !fichier.isEmpty()
            ? documentStorageService.stocker(fichier) : null;
        Document document = Document.creer(typeDocumentId, salarieId, entrepriseId, chantierId, fichierUrl,
            stocke != null ? stocke.nomFichierOriginal() : null, stocke != null ? stocke.typeMime() : null,
            stocke != null ? stocke.tailleOctets() : null, stocke != null ? stocke.cheminStockage() : null,
            dateDebutValidite, dateExpiration, dateRelance, mentions);
        document = documentRepository.save(document);
        Map<String, Object> details = new HashMap<>();
        details.put("typeDocumentLibelle", libelleType(typeDocumentId));
        details.put("nomFichier", stocke != null ? stocke.nomFichierOriginal() : fichierUrl);
        details.put("dateExpiration", dateExpiration);
        historiqueService.enregistrer(ENTITE, document.getId(), "CREATION", details);
        return document;
    }

    @Transactional(readOnly = true)
    public List<Document> listerParSalarie(UUID salarieId) {
        return documentRepository.findBySalarieId(salarieId);
    }

    /** Les documents d'un salarié propres à un chantier précis — instance indépendante de
        ses documents globaux et de ceux de ses autres chantiers (voir
        DocumentRepository.findBySalarieIdAndChantierId : modifier/supprimer/refuser ici ne
        touche jamais un autre chantier, chacun a sa propre ligne). */
    @Transactional(readOnly = true)
    public List<Document> listerParSalarieEtChantier(UUID salarieId, UUID chantierId) {
        return documentRepository.findBySalarieIdAndChantierId(salarieId, chantierId);
    }

    @Transactional(readOnly = true)
    public List<Document> listerParEntreprise(UUID entrepriseId) {
        return documentRepository.findByEntrepriseId(entrepriseId);
    }

    /** Les documents d'une entreprise propres à un chantier précis — instance indépendante
        de ses documents globaux et de ceux de ses autres chantiers (voir
        DocumentRepository.findByEntrepriseIdAndChantierId : modifier/supprimer/refuser ici
        ne touche jamais un autre chantier, chacun a sa propre ligne). */
    @Transactional(readOnly = true)
    public List<Document> listerParEntrepriseEtChantier(UUID entrepriseId, UUID chantierId) {
        return documentRepository.findByEntrepriseIdAndChantierId(entrepriseId, chantierId);
    }

    public Document valider(UUID id, LocalDate dateDebutValidite, LocalDate dateExpiration) {
        Document document = obtenir(id);
        typeDocumentRepository.findById(document.getTypeDocumentId()).ifPresent(type -> {
            if (type.isDateDebutValiditeRequise() && dateDebutValidite == null) {
                throw new BusinessRuleViolationException("La date de début de validité est obligatoire pour valider ce document");
            }
            if (type.isDateFinValiditeRequise() && dateExpiration == null) {
                throw new BusinessRuleViolationException("La date de fin de validité est obligatoire pour valider ce document");
            }
        });
        String ancienStatut = document.getStatutValidation().name();
        document.valider(dateDebutValidite, dateExpiration);
        Map<String, Object> details = new HashMap<>();
        details.put("typeDocumentLibelle", libelleType(document.getTypeDocumentId()));
        details.put("ancienStatut", ancienStatut);
        details.put("nouveauStatut", document.getStatutValidation().name());
        historiqueService.enregistrer(ENTITE, id, "VALIDATION", details);
        return document;
    }

    public Document refuser(UUID id, UUID documentEtatId) {
        Document document = obtenir(id);
        String ancienStatut = document.getStatutValidation().name();
        document.refuser(documentEtatId);
        Map<String, Object> details = new HashMap<>();
        details.put("typeDocumentLibelle", libelleType(document.getTypeDocumentId()));
        details.put("ancienStatut", ancienStatut);
        details.put("nouveauStatut", document.getStatutValidation().name());
        details.put("motif", libelleEtat(documentEtatId));
        historiqueService.enregistrer(ENTITE, id, "REFUS", details);
        return document;
    }

    public void notifier(UUID id, String email, String sujet, String description) {
        Document document = obtenir(id);
        String type = document.getSalarieId() != null ? "SALARIE" : "ENTREPRISE";
        UUID entiteId = document.getSalarieId() != null ? document.getSalarieId() : document.getEntrepriseId();
        String lien = "%s/documents?type=%s&entiteId=%s&documentId=%s".formatted(frontendBaseUrl, type, entiteId, id);
        String corps = description + "\n\nAccéder au document : " + lien;
        emailService.envoyer(email, sujet, corps);

        // Trace la relance manuelle dans message_planifie (statut ENVOYE immédiat)
        // pour qu'elle apparaisse dans l'historique "Relances" du document/salarié,
        // au même titre que les relances automatiques déclenchées par une règle.
        UUID entrepriseDestinataireId = document.getEntrepriseId() != null
            ? document.getEntrepriseId()
            : salarieRepository.findById(document.getSalarieId()).map(s -> s.getEntrepriseEmployeurId()).orElse(null);
        if (entrepriseDestinataireId != null) {
            messagePlanifieRepository.save(MessagePlanifie.manuelle(currentUser.keycloakId(), id,
                DestinataireType.ENTREPRISE, entrepriseDestinataireId, sujet, description));
        }
    }

    /** chantierId : même principe que listerParEntrepriseEtChantier/listerParSalarieEtChantier
        — un salarié/une entreprise sur plusieurs chantiers a des documents indépendants par
        chantier, donc son historique doit l'être aussi (sinon l'historique du chantier A
        montre aussi les créations/validations/refus survenus sur le chantier B). Null =
        comportement global inchangé (tous chantiers confondus). */
    @Transactional(readOnly = true)
    public List<HistoriqueModificationResponse> listerHistoriqueParSalarie(UUID salarieId, UUID chantierId) {
        List<Document> documents = chantierId != null
            ? documentRepository.findBySalarieIdAndChantierId(salarieId, chantierId)
            : documentRepository.findBySalarieId(salarieId);
        return historiqueService.listerPour(ENTITE, documents.stream().map(Document::getId).toList());
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationResponse> listerHistoriqueParEntreprise(UUID entrepriseId, UUID chantierId) {
        List<Document> documents = chantierId != null
            ? documentRepository.findByEntrepriseIdAndChantierId(entrepriseId, chantierId)
            : documentRepository.findByEntrepriseId(entrepriseId);
        return historiqueService.listerPour(ENTITE, documents.stream().map(Document::getId).toList());
    }

    @Transactional(readOnly = true)
    public List<MessagePlanifieResponse> listerRelancesParSalarie(UUID salarieId, UUID chantierId) {
        List<Document> documents = chantierId != null
            ? documentRepository.findBySalarieIdAndChantierId(salarieId, chantierId)
            : documentRepository.findBySalarieId(salarieId);
        return listerRelancesPour(documents.stream().map(Document::getId).toList());
    }

    @Transactional(readOnly = true)
    public List<MessagePlanifieResponse> listerRelancesParEntreprise(UUID entrepriseId, UUID chantierId) {
        List<Document> documents = chantierId != null
            ? documentRepository.findByEntrepriseIdAndChantierId(entrepriseId, chantierId)
            : documentRepository.findByEntrepriseId(entrepriseId);
        return listerRelancesPour(documents.stream().map(Document::getId).toList());
    }

    private List<MessagePlanifieResponse> listerRelancesPour(List<UUID> documentIds) {
        if (documentIds.isEmpty()) {
            return List.of();
        }
        return messagePlanifieRepository.findBySourceEntityIdInOrderByCreatedAtDesc(documentIds).stream()
            .map(MessagePlanifieResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentEnAttenteResponse> listerEnAttente() {
        return documentRepository.findByStatutValidationOrderByCreatedAtDesc(StatutValidation.EN_ATTENTE).stream()
            .map(d -> new DocumentEnAttenteResponse(
                d.getId(),
                d.getTypeDocumentId(),
                libelleType(d.getTypeDocumentId()),
                d.getSalarieId(),
                d.getSalarieId() != null ? nomSalarie(d.getSalarieId()) : null,
                d.getEntrepriseId(),
                d.getEntrepriseId() != null ? nomEntreprise(d.getEntrepriseId()) : null,
                d.getCreatedAt()
            ))
            .toList();
    }

    /** Documents déjà validés dont la date d'expiration tombe dans les {@code joursSeuil}
        prochains jours (tous portefeuilles confondus) — sert au KPI "Documents expirant"
        et au bloc "Échéances à venir" du tableau de bord Admin, qui n'avaient jusqu'ici
        aucune vue portefeuille (seule la fiche d'une entreprise/d'un salarié montrait ses
        propres expirations, une à la fois). */
    @Transactional(readOnly = true)
    public List<DocumentExpirantResponse> listerExpirantBientot(int joursSeuil) {
        LocalDate aujourdHui = LocalDate.now();
        return documentRepository.findByStatutValidationAndDateExpirationBetweenOrderByDateExpirationAsc(
                StatutValidation.VALIDE, aujourdHui, aujourdHui.plusDays(joursSeuil)).stream()
            .map(d -> new DocumentExpirantResponse(
                d.getId(),
                d.getTypeDocumentId(),
                libelleType(d.getTypeDocumentId()),
                d.getSalarieId(),
                d.getSalarieId() != null ? nomSalarie(d.getSalarieId()) : null,
                d.getEntrepriseId(),
                d.getEntrepriseId() != null ? nomEntreprise(d.getEntrepriseId()) : null,
                d.getDateExpiration()
            ))
            .toList();
    }

    /** Combien d'entreprises actives ont, pour chacun de leurs types de documents
        obligatoires (filtrés par corps de métier / pays comme sur la fiche entreprise,
        voir EntrepriseDetailComponent#recalculerTypesPourEntreprise côté frontend, jamais
        dupliqué jusqu'ici côté backend), un document au statut VALIDE. Sert au KPI
        "Entreprises à jour" du tableau de bord Admin plutôt qu'un total brut d'entreprises. */
    @Transactional(readOnly = true)
    public ConformitePortefeuilleResponse calculerConformitePortefeuille() {
        List<Entreprise> entreprises = entrepriseRepository.findByActifTrue();
        List<TypeDocument> typesObligatoires = typeDocumentRepository.findAll().stream()
            .filter(t -> t.getCible() == CibleDocument.ENTREPRISE && t.isObligatoire())
            .toList();

        Map<UUID, Set<UUID>> typesValidesParEntreprise = new HashMap<>();
        for (Document d : documentRepository.findByEntrepriseIdIsNotNull()) {
            if (d.getStatutValidation() == StatutValidation.VALIDE) {
                typesValidesParEntreprise.computeIfAbsent(d.getEntrepriseId(), k -> new HashSet<>()).add(d.getTypeDocumentId());
            }
        }

        int conformes = 0;
        for (Entreprise entreprise : entreprises) {
            List<TypeDocument> typesPourEntreprise = typesObligatoires.stream()
                .filter(t -> t.getCorpsDeMetierId() == null || t.getCorpsDeMetierId().equals(entreprise.getCorpsDeMetierId()))
                .filter(t -> t.getPaysId() == null || t.getPaysId().equals(entreprise.getPaysId()))
                .toList();
            Set<UUID> valides = typesValidesParEntreprise.getOrDefault(entreprise.getId(), Set.of());
            boolean estConforme = typesPourEntreprise.stream().allMatch(t -> valides.contains(t.getId()));
            if (estConforme) {
                conformes++;
            }
        }
        return new ConformitePortefeuilleResponse(conformes, entreprises.size());
    }

    public void supprimer(UUID id) {
        Document document = obtenir(id);
        Map<String, Object> details = new HashMap<>();
        details.put("typeDocumentLibelle", libelleType(document.getTypeDocumentId()));
        historiqueService.enregistrer(ENTITE, id, "SUPPRESSION", details);
        document.supprimer();
    }

    @Transactional(readOnly = true)
    public Document obtenir(UUID id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Document", id));
    }

    /** @return le contenu binaire du fichier déposé sur ce document, jamais null (404 si aucun fichier n'a été déposé). */
    @Transactional(readOnly = true)
    public Resource chargerFichier(UUID id) {
        Document document = obtenir(id);
        if (document.getCheminStockage() == null) {
            throw new EntityNotFoundException("Aucun fichier n'a été déposé pour ce document");
        }
        return documentStorageService.charger(document.getCheminStockage());
    }

    private String libelleType(UUID typeDocumentId) {
        return typeDocumentRepository.findById(typeDocumentId).map(t -> t.getLibelle()).orElse(null);
    }

    private String libelleEtat(UUID documentEtatId) {
        if (documentEtatId == null) {
            return null;
        }
        return documentEtatRepository.findById(documentEtatId).map(e -> e.getTitre()).orElse(null);
    }

    private String nomSalarie(UUID salarieId) {
        return salarieRepository.findById(salarieId).map(s -> s.getPrenom() + " " + s.getNom()).orElse(null);
    }

    private String nomEntreprise(UUID entrepriseId) {
        return entrepriseRepository.findById(entrepriseId).map(e -> e.getRaisonSociale()).orElse(null);
    }
}
