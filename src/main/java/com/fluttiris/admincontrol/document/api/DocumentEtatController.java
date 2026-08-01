package com.fluttiris.admincontrol.document.api;

import com.fluttiris.admincontrol.document.api.dto.CreateDocumentEtatRequest;
import com.fluttiris.admincontrol.document.api.dto.DocumentEtatResponse;
import com.fluttiris.admincontrol.document.application.DocumentEtatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents-etats")
@RequiredArgsConstructor
public class DocumentEtatController {

    private final DocumentEtatService documentEtatService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DocumentEtatResponse> creer(@Valid @RequestBody CreateDocumentEtatRequest request) {
        var etat = documentEtatService.creer(request.titre(), request.parDefaut(), request.dateExpiree(), request.valideLeDocument());
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentEtatResponse.from(etat));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<DocumentEtatResponse> lister() {
        return documentEtatService.lister().stream().map(DocumentEtatResponse::from).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DocumentEtatResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateDocumentEtatRequest request) {
        var etat = documentEtatService.modifier(id, request.titre(), request.parDefaut(), request.dateExpiree(), request.valideLeDocument());
        return DocumentEtatResponse.from(etat);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        documentEtatService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
