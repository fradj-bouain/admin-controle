package com.fluttiris.admincontrol.messagerie.api;

import com.fluttiris.admincontrol.messagerie.api.dto.CreateRegleAutomatisationRequest;
import com.fluttiris.admincontrol.messagerie.api.dto.RegleAutomatisationResponse;
import com.fluttiris.admincontrol.messagerie.application.RegleAutomatisationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/regles-automatisation")
@RequiredArgsConstructor
public class RegleAutomatisationController {

    private final RegleAutomatisationService regleAutomatisationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<RegleAutomatisationResponse> creer(@Valid @RequestBody CreateRegleAutomatisationRequest request) {
        var regle = regleAutomatisationService.creer(request.nom(), request.evenementDeclencheur(), request.nbJoursAvant(),
            request.cibleGroupe(), request.destinataireType(), request.destinataireId(), request.sujet(), request.contenu());
        return ResponseEntity.status(HttpStatus.CREATED).body(RegleAutomatisationResponse.from(regle));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<RegleAutomatisationResponse> lister() {
        return regleAutomatisationService.lister().stream().map(RegleAutomatisationResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public RegleAutomatisationResponse obtenir(@PathVariable UUID id) {
        return RegleAutomatisationResponse.from(regleAutomatisationService.obtenir(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public RegleAutomatisationResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateRegleAutomatisationRequest request) {
        var regle = regleAutomatisationService.modifier(id, request.nom(), request.evenementDeclencheur(),
            request.nbJoursAvant(), request.cibleGroupe(), request.destinataireType(), request.destinataireId(),
            request.sujet(), request.contenu());
        return RegleAutomatisationResponse.from(regle);
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public RegleAutomatisationResponse activer(@PathVariable UUID id) {
        return RegleAutomatisationResponse.from(regleAutomatisationService.activer(id));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public RegleAutomatisationResponse desactiver(@PathVariable UUID id) {
        return RegleAutomatisationResponse.from(regleAutomatisationService.desactiver(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        regleAutomatisationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
