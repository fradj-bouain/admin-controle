package com.fluttiris.admincontrol.document.api;

import com.fluttiris.admincontrol.document.api.dto.AjouterDocumentChantierSupplementaireRequest;
import com.fluttiris.admincontrol.document.api.dto.DocumentChantierSupplementaireResponse;
import com.fluttiris.admincontrol.document.application.DocumentChantierSupplementaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Types de document demandés EN PLUS sur un chantier précis (au-delà des types
 * obligatoires globaux, qui s'appliquent déjà partout) — réservé au SUPER_ADMIN, au
 * même titre que la gestion des affectations entreprise↔chantier (voir
 * AffectationEntrepriseChantierController) : c'est le donneur d'ordre qui décide des
 * pièces exigées sur son chantier, pas l'entreprise elle-même.
 */
@RestController
@RequestMapping("/api/v1/chantiers/{chantierId}/documents-supplementaires")
@RequiredArgsConstructor
public class DocumentChantierSupplementaireController {

    private final DocumentChantierSupplementaireService service;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<DocumentChantierSupplementaireResponse> lister(@PathVariable UUID chantierId) {
        return service.listerParChantier(chantierId).stream()
            .map(DocumentChantierSupplementaireResponse::from)
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DocumentChantierSupplementaireResponse> ajouter(
        @PathVariable UUID chantierId, @Valid @RequestBody AjouterDocumentChantierSupplementaireRequest request) {
        var regle = service.ajouter(chantierId, request.typeDocumentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentChantierSupplementaireResponse.from(regle));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> retirer(@PathVariable UUID chantierId, @PathVariable UUID id) {
        service.retirer(id);
        return ResponseEntity.noContent().build();
    }
}
