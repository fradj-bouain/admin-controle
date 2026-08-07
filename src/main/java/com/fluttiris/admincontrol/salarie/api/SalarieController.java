package com.fluttiris.admincontrol.salarie.api;

import com.fluttiris.admincontrol.salarie.api.dto.AffectationSalarieChantierResponse;
import com.fluttiris.admincontrol.salarie.api.dto.CreateSalarieRequest;
import com.fluttiris.admincontrol.salarie.api.dto.SalarieResponse;
import com.fluttiris.admincontrol.salarie.application.AffectationSalarieChantierService;
import com.fluttiris.admincontrol.salarie.application.SalarieService;
import com.fluttiris.admincontrol.salarie.domain.Salarie;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/salaries")
@RequiredArgsConstructor
public class SalarieController {

    private final SalarieService salarieService;
    private final AffectationSalarieChantierService affectationSalarieChantierService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#request.entrepriseEmployeurId()))")
    public ResponseEntity<SalarieResponse> creer(@Valid @RequestBody CreateSalarieRequest request) {
        var salarie = salarieService.creer(request.nom(), request.prenom(), request.dateNaissance(),
            request.nationalitePaysId(), request.entrepriseEmployeurId(), request.typeSalarieId(),
            request.typeContratId(), request.fonctionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SalarieResponse.from(salarie));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.salarieAppartientAEntreprise(#id, @currentUser.entrepriseId().get()) "
        + "and @currentUser.entrepriseId().get().equals(#request.entrepriseEmployeurId()))")
    public SalarieResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateSalarieRequest request) {
        var salarie = salarieService.modifier(id, request.nom(), request.prenom(), request.dateNaissance(),
            request.nationalitePaysId(), request.entrepriseEmployeurId(), request.typeSalarieId(),
            request.typeContratId(), request.fonctionId());
        return SalarieResponse.from(salarie);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.salarieAppartientAEntreprise(#id, @currentUser.entrepriseId().get())) or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.salarieAccessibleParClient(#id, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isEmpty() and @currentUser.clientId().isEmpty())")
    public SalarieResponse obtenir(@PathVariable UUID id) {
        return SalarieResponse.from(salarieService.obtenir(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SalarieResponse> lister(@RequestParam(required = false) UUID entrepriseId) {
        // Un compte Client est un tenant : uniquement les salariés affectés aux chantiers
        // qui lui ont été explicitement assignés (voir ChantierService.listerAccessiblesPourClient) —
        // aucun chantier assigné = aucun salarié visible.
        if (currentUser.clientId().isPresent()) {
            return avecChantierActuel(salarieService.listerParClient(currentUser.clientId().get(), currentUser.keycloakId()));
        }
        // Un compte Entreprise est toujours ramené à SON propre périmètre, même
        // s'il passe un autre entrepriseId en paramètre.
        UUID scope = currentUser.entrepriseId().orElse(entrepriseId);
        if (scope != null) {
            return avecChantierActuel(salarieService.lister(scope));
        }
        return avecChantierActuel(salarieService.lister(null));
    }

    private List<SalarieResponse> avecChantierActuel(List<Salarie> salaries) {
        var chantierActuelParSalarie = salarieService.chantierActuelParSalarie(salaries.stream()
            .map(Salarie::getId).toList());
        return salaries.stream()
            .map(s -> SalarieResponse.from(s, chantierActuelParSalarie.get(s.getId())))
            .toList();
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.salarieAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public SalarieResponse desactiver(@PathVariable UUID id) {
        return SalarieResponse.from(salarieService.desactiver(id));
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.salarieAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public SalarieResponse activer(@PathVariable UUID id) {
        return SalarieResponse.from(salarieService.activer(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.salarieAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        salarieService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/chantiers")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.salarieAppartientAEntreprise(#id, @currentUser.entrepriseId().get())) or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.salarieAccessibleParClient(#id, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isEmpty() and @currentUser.clientId().isEmpty())")
    public List<AffectationSalarieChantierResponse> listerChantiers(@PathVariable UUID id) {
        return affectationSalarieChantierService.listerParSalarie(id).stream()
            .map(a -> AffectationSalarieChantierResponse.from(a,
                affectationSalarieChantierService.entrepriseIdDeLAffectation(a.getAffectationEntrepriseChantierId())))
            .toList();
    }
}
