package com.fluttiris.admincontrol.entreprise.api;

import com.fluttiris.admincontrol.entreprise.api.dto.AffectationEntrepriseChantierResponse;
import com.fluttiris.admincontrol.entreprise.api.dto.AffecterEntrepriseRequest;
import com.fluttiris.admincontrol.entreprise.application.AffectationEntrepriseChantierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Auto-gestion des intervenants par les entreprises (exigence client) : une
 * entreprise Principale ou STT1 peut affecter/désactiver ses propres
 * sous-traitants SUR CE CHANTIER, sans passer par un admin. La vérification
 * est contextuelle (voir ChantierAuthorizationService) : le même utilisateur
 * peut avoir ce droit sur le chantier A et ne pas l'avoir sur le chantier B.
 */
@RestController
@RequestMapping("/api/v1/chantiers/{chantierId}/entreprises")
@RequiredArgsConstructor
public class AffectationEntrepriseChantierController {

    private final AffectationEntrepriseChantierService affectationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.canManageSousTraitants(#chantierId, @currentUser.entrepriseId().get()))")
    public ResponseEntity<AffectationEntrepriseChantierResponse> affecter(
        @PathVariable UUID chantierId, @Valid @RequestBody AffecterEntrepriseRequest request) {
        var affectation = affectationService.affecter(
            chantierId, request.entrepriseId(), request.role(), request.affectationParenteId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AffectationEntrepriseChantierResponse.from(affectation));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AffectationEntrepriseChantierResponse> lister(@PathVariable UUID chantierId) {
        return affectationService.listerParChantier(chantierId).stream()
            .map(AffectationEntrepriseChantierResponse::from)
            .toList();
    }

    @PostMapping("/{affectationId}/desactiver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.canManageSousTraitants(#chantierId, @currentUser.entrepriseId().get()))")
    public ResponseEntity<Void> desactiver(@PathVariable UUID chantierId, @PathVariable UUID affectationId) {
        affectationService.desactiver(affectationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{affectationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.canManageSousTraitants(#chantierId, @currentUser.entrepriseId().get()))")
    public ResponseEntity<Void> supprimer(@PathVariable UUID chantierId, @PathVariable UUID affectationId) {
        affectationService.supprimer(affectationId);
        return ResponseEntity.noContent().build();
    }
}
