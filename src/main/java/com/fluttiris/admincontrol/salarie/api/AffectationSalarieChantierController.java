package com.fluttiris.admincontrol.salarie.api;

import com.fluttiris.admincontrol.salarie.api.dto.AffectationSalarieChantierResponse;
import com.fluttiris.admincontrol.salarie.api.dto.AffecterSalarieRequest;
import com.fluttiris.admincontrol.salarie.api.dto.MajSuiviAffectationRequest;
import com.fluttiris.admincontrol.salarie.api.dto.RefuserAccesRequest;
import com.fluttiris.admincontrol.salarie.application.AffectationSalarieChantierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chantiers/{chantierId}/salaries")
@RequiredArgsConstructor
public class AffectationSalarieChantierController {

    private final AffectationSalarieChantierService affectationService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.canManageOwnSalaries(#chantierId, @currentUser.entrepriseId().get()))")
    public ResponseEntity<AffectationSalarieChantierResponse> affecter(
        @PathVariable UUID chantierId, @Valid @RequestBody AffecterSalarieRequest request) {
        var affectation = affectationService.affecter(request.salarieId(), chantierId, request.affectationEntrepriseChantierId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
            AffectationSalarieChantierResponse.from(affectation, affectationService.entrepriseIdDeLAffectation(affectation.getAffectationEntrepriseChantierId())));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AffectationSalarieChantierResponse> lister(@PathVariable UUID chantierId) {
        return affectationService.listerParChantier(chantierId).stream()
            .map(a -> AffectationSalarieChantierResponse.from(a, affectationService.entrepriseIdDeLAffectation(a.getAffectationEntrepriseChantierId())))
            .toList();
    }

    @PostMapping("/{affectationId}/accorder-acces")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AffectationSalarieChantierResponse accorderAcces(@PathVariable UUID chantierId, @PathVariable UUID affectationId) {
        var affectation = affectationService.accorderAcces(affectationId);
        return AffectationSalarieChantierResponse.from(affectation, affectationService.entrepriseIdDeLAffectation(affectation.getAffectationEntrepriseChantierId()));
    }

    @PostMapping("/{affectationId}/refuser-acces")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AffectationSalarieChantierResponse refuserAcces(@PathVariable UUID chantierId, @PathVariable UUID affectationId,
                                                             @RequestBody(required = false) RefuserAccesRequest request) {
        String motif = request != null ? request.motif() : null;
        var affectation = affectationService.refuserAcces(affectationId, motif);
        return AffectationSalarieChantierResponse.from(affectation, affectationService.entrepriseIdDeLAffectation(affectation.getAffectationEntrepriseChantierId()));
    }

    @PostMapping("/{affectationId}/cloturer")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.canManageOwnSalaries(#chantierId, @currentUser.entrepriseId().get()))")
    public AffectationSalarieChantierResponse cloturer(@PathVariable UUID chantierId, @PathVariable UUID affectationId) {
        var affectation = affectationService.cloturer(affectationId);
        return AffectationSalarieChantierResponse.from(affectation, affectationService.entrepriseIdDeLAffectation(affectation.getAffectationEntrepriseChantierId()));
    }

    @DeleteMapping("/{affectationId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.canManageOwnSalaries(#chantierId, @currentUser.entrepriseId().get()))")
    public ResponseEntity<Void> supprimer(@PathVariable UUID chantierId, @PathVariable UUID affectationId) {
        affectationService.supprimer(affectationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{affectationId}/maj-suivi")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.canManageOwnSalaries(#chantierId, @currentUser.entrepriseId().get()))")
    public AffectationSalarieChantierResponse majSuivi(@PathVariable UUID chantierId, @PathVariable UUID affectationId,
                                                         @RequestBody MajSuiviAffectationRequest request) {
        var affectation = affectationService.majSuivi(affectationId, request.epiGants(), request.epiCasque(),
            request.epiChaussures(), request.badgeEdite(), request.present());
        return AffectationSalarieChantierResponse.from(affectation, affectationService.entrepriseIdDeLAffectation(affectation.getAffectationEntrepriseChantierId()));
    }
}
