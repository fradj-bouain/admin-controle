package com.fluttiris.admincontrol.document.application;

import com.fluttiris.admincontrol.common.audit.HistoriqueModificationResponse;
import com.fluttiris.admincontrol.common.audit.HistoriqueModificationService;
import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.common.mail.EmailService;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import com.fluttiris.admincontrol.document.api.dto.DocumentEnAttenteResponse;
import com.fluttiris.admincontrol.document.domain.Document;
import com.fluttiris.admincontrol.document.domain.DocumentEtatRepository;
import com.fluttiris.admincontrol.document.domain.DocumentRepository;
import com.fluttiris.admincontrol.document.domain.StatutValidation;
import com.fluttiris.admincontrol.document.domain.TypeDocumentRepository;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import com.fluttiris.admincontrol.messagerie.api.dto.MessagePlanifieResponse;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifieRepository;
import com.fluttiris.admincontrol.salarie.domain.SalarieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final CurrentUser currentUser;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public Document creer(UUID typeDocumentId, UUID salarieId, UUID entrepriseId, UUID chantierId,
                           String fichierUrl, LocalDate dateDebutValidite, LocalDate dateExpiration,
                           LocalDate dateRelance, String mentions) {
        Document document = Document.creer(typeDocumentId, salarieId, entrepriseId, chantierId, fichierUrl,
            dateDebutValidite, dateExpiration, dateRelance, mentions);
        document = documentRepository.save(document);
        Map<String, Object> details = new HashMap<>();
        details.put("typeDocumentLibelle", libelleType(typeDocumentId));
        details.put("fichierUrl", fichierUrl);
        details.put("dateExpiration", dateExpiration);
        historiqueService.enregistrer(ENTITE, document.getId(), "CREATION", details);
        return document;
    }

    @Transactional(readOnly = true)
    public List<Document> listerParSalarie(UUID salarieId) {
        return documentRepository.findBySalarieId(salarieId);
    }

    @Transactional(readOnly = true)
    public List<Document> listerParEntreprise(UUID entrepriseId) {
        return documentRepository.findByEntrepriseId(entrepriseId);
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

    @Transactional(readOnly = true)
    public List<HistoriqueModificationResponse> listerHistoriqueParSalarie(UUID salarieId) {
        List<UUID> documentIds = documentRepository.findBySalarieId(salarieId).stream().map(Document::getId).toList();
        return historiqueService.listerPour(ENTITE, documentIds);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationResponse> listerHistoriqueParEntreprise(UUID entrepriseId) {
        List<UUID> documentIds = documentRepository.findByEntrepriseId(entrepriseId).stream().map(Document::getId).toList();
        return historiqueService.listerPour(ENTITE, documentIds);
    }

    @Transactional(readOnly = true)
    public List<MessagePlanifieResponse> listerRelancesParSalarie(UUID salarieId) {
        List<UUID> documentIds = documentRepository.findBySalarieId(salarieId).stream().map(Document::getId).toList();
        return listerRelancesPour(documentIds);
    }

    @Transactional(readOnly = true)
    public List<MessagePlanifieResponse> listerRelancesParEntreprise(UUID entrepriseId) {
        List<UUID> documentIds = documentRepository.findByEntrepriseId(entrepriseId).stream().map(Document::getId).toList();
        return listerRelancesPour(documentIds);
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
                libelleType(d.getTypeDocumentId()),
                d.getSalarieId(),
                d.getSalarieId() != null ? nomSalarie(d.getSalarieId()) : null,
                d.getEntrepriseId(),
                d.getEntrepriseId() != null ? nomEntreprise(d.getEntrepriseId()) : null,
                d.getCreatedAt()
            ))
            .toList();
    }

    public void supprimer(UUID id) {
        Document document = obtenir(id);
        Map<String, Object> details = new HashMap<>();
        details.put("typeDocumentLibelle", libelleType(document.getTypeDocumentId()));
        historiqueService.enregistrer(ENTITE, id, "SUPPRESSION", details);
        document.supprimer();
    }

    private Document obtenir(UUID id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Document", id));
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
