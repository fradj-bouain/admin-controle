package com.fluttiris.admincontrol.entreprise.api;

import com.fluttiris.admincontrol.entreprise.api.dto.AffectationEntrepriseChantierResponse;
import com.fluttiris.admincontrol.entreprise.api.dto.AffecterEntrepriseRequest;
import com.fluttiris.admincontrol.entreprise.application.AffectationEntrepriseChantierService;
import com.fluttiris.admincontrol.entreprise.application.EntrepriseService;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion des affectations entreprise↔chantier (rang PRINCIPALE/STT1/STT2,
 * sous-traitants) — réservée au SUPER_ADMIN. Une entreprise pouvait auparavant
 * gérer ses propres sous-traitants sans passer par un admin (voir
 * ChantierAuthorizationService.canManageSousTraitants) ; retiré à la demande
 * du client réel du projet, qui veut garder la main sur qui intervient sur
 * son chantier. La lecture reste ouverte à l'entreprise affectée (elle doit
 * pouvoir voir qui est sur son chantier, juste plus le gérer).
 */
@RestController
@RequestMapping("/api/v1/chantiers/{chantierId}/entreprises")
@RequiredArgsConstructor
public class AffectationEntrepriseChantierController {

    private final AffectationEntrepriseChantierService affectationService;
    private final EntrepriseService entrepriseService;

    // La gestion des affectations entreprise↔chantier (rang, sous-traitants) est réservée au
    // SUPER_ADMIN : c'est le client (donneur d'ordre) qui décide qui intervient sur son chantier,
    // pas l'entreprise elle-même — retiré à la demande du client réel du projet (voir historique).
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AffectationEntrepriseChantierResponse> affecter(
        @PathVariable UUID chantierId, @Valid @RequestBody AffecterEntrepriseRequest request) {
        var affectation = affectationService.affecter(
            chantierId, request.entrepriseId(), request.role(), request.affectationParenteId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AffectationEntrepriseChantierResponse.from(affectation));
    }

    /** Un compte Client ne doit voir les entreprises d'un chantier QUE si ce chantier lui
        est accessible (voir scopeAuthz.chantierAccessibleParClient) — sans ce garde-fou,
        n'importe quel compte Client pouvait consulter les entreprises de N'IMPORTE QUEL
        chantier en devinant son id dans l'URL (isEmpty() couvrait aussi bien Client que
        Contrôleur). Comportement Entreprise/Contrôleur/SUPER_ADMIN inchangé. */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.isAffectedToChantier(#chantierId, @currentUser.entrepriseId().get())) or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.chantierAccessibleParClient(#chantierId, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isEmpty() and @currentUser.clientId().isEmpty())")
    public List<AffectationEntrepriseChantierResponse> lister(@PathVariable UUID chantierId) {
        List<AffectationEntrepriseChantier> affectations = affectationService.listerParChantier(chantierId);
        Map<UUID, String> raisonSocialeParId = entrepriseService.raisonSocialeParEntrepriseIds(
            affectations.stream().map(AffectationEntrepriseChantier::getEntrepriseId).toList());
        return affectations.stream()
            .map(a -> AffectationEntrepriseChantierResponse.from(a, raisonSocialeParId.get(a.getEntrepriseId())))
            .toList();
    }

    @PostMapping("/{affectationId}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> desactiver(@PathVariable UUID chantierId, @PathVariable UUID affectationId) {
        affectationService.desactiver(affectationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{affectationId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID chantierId, @PathVariable UUID affectationId) {
        affectationService.supprimer(affectationId);
        return ResponseEntity.noContent().build();
    }
}
