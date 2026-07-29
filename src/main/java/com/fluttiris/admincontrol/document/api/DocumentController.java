package com.fluttiris.admincontrol.document.api;

import com.fluttiris.admincontrol.document.api.dto.CreateDocumentRequest;
import com.fluttiris.admincontrol.document.api.dto.DocumentResponse;
import com.fluttiris.admincontrol.document.application.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> creer(@Valid @RequestBody CreateDocumentRequest request) {
        var document = documentService.creer(request.typeDocumentId(), request.salarieId(), request.entrepriseId(),
            request.chantierId(), request.fichierUrl(), request.dateDebutValidite(), request.dateExpiration(),
            request.dateRelance(), request.mentions());
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<DocumentResponse> lister(@RequestParam(required = false) UUID salarieId,
                                          @RequestParam(required = false) UUID entrepriseId) {
        if (salarieId != null) {
            return documentService.listerParSalarie(salarieId).stream().map(DocumentResponse::from).toList();
        }
        if (entrepriseId != null) {
            return documentService.listerParEntreprise(entrepriseId).stream().map(DocumentResponse::from).toList();
        }
        return List.of();
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public DocumentResponse valider(@PathVariable UUID id) {
        return DocumentResponse.from(documentService.valider(id));
    }

    @PostMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public DocumentResponse refuser(@PathVariable UUID id) {
        return DocumentResponse.from(documentService.refuser(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        documentService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
