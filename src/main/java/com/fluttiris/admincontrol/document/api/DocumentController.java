package com.fluttiris.admincontrol.document.api;

import com.fluttiris.admincontrol.common.audit.HistoriqueModificationResponse;
import com.fluttiris.admincontrol.document.api.dto.CreateDocumentRequest;
import com.fluttiris.admincontrol.document.api.dto.DocumentEnAttenteResponse;
import com.fluttiris.admincontrol.document.api.dto.DocumentResponse;
import com.fluttiris.admincontrol.document.api.dto.NotifierDocumentRequest;
import com.fluttiris.admincontrol.document.api.dto.RefuserDocumentRequest;
import com.fluttiris.admincontrol.document.api.dto.ValiderDocumentRequest;
import com.fluttiris.admincontrol.document.application.DocumentService;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import com.fluttiris.admincontrol.common.security.ScopeAuthorizationService;
import com.fluttiris.admincontrol.messagerie.api.dto.MessagePlanifieResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUser currentUser;
    private final ScopeAuthorizationService scopeAuthz;

    // Le SUPER_ADMIN ne dépose jamais de document lui-même — son rôle se limite à
    // valider/refuser ce que l'entreprise (ou le salarié via l'entreprise) a déposé.
    // Décision confirmée explicitement : avant cette version, l'endpoint restait
    // ouvert à un éventuel appel SUPER_ADMIN "légitime" qui ne s'est jamais présenté
    // et que l'UI n'était de toute façon pas censée exposer.
    @PostMapping
    @PreAuthorize("isAuthenticated() and !@currentUser.estAdmin()")
    public ResponseEntity<DocumentResponse> creer(@Valid @RequestBody CreateDocumentRequest request) {
        // Les dates de validité ne sont saisies que par l'administrateur, au moment de la
        // validation (voir #valider) — jamais par l'entreprise/le salarié au dépôt. L'appelant
        // ne peut plus être admin ici (voir @PreAuthorize ci-dessus), donc toujours ignorées.
        var document = documentService.creer(request.typeDocumentId(), request.salarieId(), request.entrepriseId(),
            request.chantierId(), request.fichierUrl(), null, null,
            request.dateRelance(), request.mentions());
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<DocumentResponse> lister(@RequestParam(required = false) UUID salarieId,
                                          @RequestParam(required = false) UUID entrepriseId) {
        // Un compte Entreprise ne peut consulter que ses propres documents ou ceux
        // de ses propres salariés — jamais ceux d'une autre entreprise.
        boolean estEntrepriseScope = currentUser.entrepriseId().isPresent();
        // Un compte Client ne peut consulter que les documents des salariés/entreprises
        // affectés à SES chantiers — jamais ceux d'une entité hors de son périmètre.
        boolean estClientScope = currentUser.clientId().isPresent();
        if (salarieId != null) {
            if (estEntrepriseScope && !scopeAuthz.salarieAppartientAEntreprise(salarieId, currentUser.entrepriseId().get())) {
                return List.of();
            }
            if (estClientScope && !scopeAuthz.salarieAccessibleParClient(salarieId, currentUser.clientId().get(), currentUser.keycloakId())) {
                return List.of();
            }
            return documentService.listerParSalarie(salarieId).stream().map(DocumentResponse::from).toList();
        }
        UUID scope = estEntrepriseScope ? currentUser.entrepriseId().get() : entrepriseId;
        if (scope != null) {
            if (estClientScope && !scopeAuthz.entrepriseAccessibleParClient(scope, currentUser.clientId().get(), currentUser.keycloakId())) {
                return List.of();
            }
            return documentService.listerParEntreprise(scope).stream().map(DocumentResponse::from).toList();
        }
        return List.of();
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DocumentResponse valider(@PathVariable UUID id, @RequestBody(required = false) ValiderDocumentRequest request) {
        LocalDate dateDebutValidite = request != null ? request.dateDebutValidite() : null;
        LocalDate dateExpiration = request != null ? request.dateExpiration() : null;
        return DocumentResponse.from(documentService.valider(id, dateDebutValidite, dateExpiration));
    }

    @PostMapping("/{id}/refuser")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DocumentResponse refuser(@PathVariable UUID id, @RequestBody(required = false) RefuserDocumentRequest request) {
        UUID documentEtatId = request != null ? request.documentEtatId() : null;
        return DocumentResponse.from(documentService.refuser(id, documentEtatId));
    }

    @PostMapping("/{id}/notifier")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> notifier(@PathVariable UUID id, @Valid @RequestBody NotifierDocumentRequest request) {
        documentService.notifier(id, request.email(), request.sujet(), request.description());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        documentService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/historique")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(#salarieId != null and @currentUser.entrepriseId().isPresent() and @scopeAuthz.salarieAppartientAEntreprise(#salarieId, @currentUser.entrepriseId().get())) or "
        + "(#entrepriseId != null and @currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#entrepriseId))")
    public List<HistoriqueModificationResponse> historique(@RequestParam(required = false) UUID salarieId,
                                                             @RequestParam(required = false) UUID entrepriseId) {
        if (salarieId != null) {
            return documentService.listerHistoriqueParSalarie(salarieId);
        }
        if (entrepriseId != null) {
            return documentService.listerHistoriqueParEntreprise(entrepriseId);
        }
        return List.of();
    }

    @GetMapping("/relances")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(#salarieId != null and @currentUser.entrepriseId().isPresent() and @scopeAuthz.salarieAppartientAEntreprise(#salarieId, @currentUser.entrepriseId().get())) or "
        + "(#entrepriseId != null and @currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#entrepriseId))")
    public List<MessagePlanifieResponse> relances(@RequestParam(required = false) UUID salarieId,
                                                   @RequestParam(required = false) UUID entrepriseId) {
        if (salarieId != null) {
            return documentService.listerRelancesParSalarie(salarieId);
        }
        if (entrepriseId != null) {
            return documentService.listerRelancesParEntreprise(entrepriseId);
        }
        return List.of();
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<DocumentEnAttenteResponse> enAttente() {
        return documentService.listerEnAttente();
    }
}
