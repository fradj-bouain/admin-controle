package com.fluttiris.admincontrol.document.application;

import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.common.mail.EmailService;
import com.fluttiris.admincontrol.document.domain.Document;
import com.fluttiris.admincontrol.document.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EmailService emailService;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public Document creer(UUID typeDocumentId, UUID salarieId, UUID entrepriseId, UUID chantierId,
                           String fichierUrl, LocalDate dateDebutValidite, LocalDate dateExpiration,
                           LocalDate dateRelance, String mentions) {
        Document document = Document.creer(typeDocumentId, salarieId, entrepriseId, chantierId, fichierUrl,
            dateDebutValidite, dateExpiration, dateRelance, mentions);
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<Document> listerParSalarie(UUID salarieId) {
        return documentRepository.findBySalarieId(salarieId);
    }

    @Transactional(readOnly = true)
    public List<Document> listerParEntreprise(UUID entrepriseId) {
        return documentRepository.findByEntrepriseId(entrepriseId);
    }

    public Document valider(UUID id) {
        Document document = obtenir(id);
        document.valider();
        return document;
    }

    public Document refuser(UUID id, UUID documentEtatId) {
        Document document = obtenir(id);
        document.refuser(documentEtatId);
        return document;
    }

    public void notifier(UUID id, String email, String sujet, String description) {
        Document document = obtenir(id);
        String type = document.getSalarieId() != null ? "SALARIE" : "ENTREPRISE";
        UUID entiteId = document.getSalarieId() != null ? document.getSalarieId() : document.getEntrepriseId();
        String lien = "%s/documents?type=%s&entiteId=%s&documentId=%s".formatted(frontendBaseUrl, type, entiteId, id);
        String corps = description + "\n\nAccéder au document : " + lien;
        emailService.envoyer(email, sujet, corps);
    }

    public void supprimer(UUID id) {
        Document document = obtenir(id);
        document.supprimer();
    }

    private Document obtenir(UUID id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Document", id));
    }
}
