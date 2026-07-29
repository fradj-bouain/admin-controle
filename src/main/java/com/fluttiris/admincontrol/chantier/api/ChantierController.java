package com.fluttiris.admincontrol.chantier.api;

import com.fluttiris.admincontrol.chantier.api.dto.ChantierResponse;
import com.fluttiris.admincontrol.chantier.api.dto.CreateChantierRequest;
import com.fluttiris.admincontrol.chantier.api.dto.DefinirControlesRequest;
import com.fluttiris.admincontrol.chantier.application.ChantierService;
import com.fluttiris.admincontrol.chantier.domain.Chantier;
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
@RequestMapping("/api/v1/chantiers")
@RequiredArgsConstructor
public class ChantierController {

    private final ChantierService chantierService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ChantierResponse> creer(@Valid @RequestBody CreateChantierRequest request) {
        Chantier chantier = chantierService.creer(
            request.nom(), request.clientId(), request.prestation(), request.adresse(), request.adresse2(),
            request.adresse3(), request.codePostal(), request.ville(), request.paysId(), request.noteInterne(),
            request.noteClient(), request.ipsAllowed(), request.dateDebut(), request.dateFinPrevue());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChantierResponse.from(chantier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ChantierResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateChantierRequest request) {
        Chantier chantier = chantierService.modifier(
            id, request.nom(), request.clientId(), request.prestation(), request.adresse(), request.adresse2(),
            request.adresse3(), request.codePostal(), request.ville(), request.paysId(), request.noteInterne(),
            request.noteClient(), request.ipsAllowed(), request.dateDebut(), request.dateFinPrevue());
        return ChantierResponse.from(chantier);
    }

    @PutMapping("/{id}/controles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ChantierResponse definirControles(@PathVariable UUID id, @RequestBody DefinirControlesRequest request) {
        Chantier chantier = chantierService.definirControles(id, request.recurrenceControles(), request.dateProchainControle());
        return ChantierResponse.from(chantier);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ChantierResponse obtenir(@PathVariable UUID id) {
        // Le scope par rôle Client (lecture seule sur SES chantiers) est appliqué
        // au niveau service dans une prochaine itération, une fois le module
        // Client relié à l'utilisateur Keycloak (client_id claim, cf. CurrentUser).
        return ChantierResponse.from(chantierService.obtenir(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ChantierResponse> lister(@RequestParam(required = false) UUID clientId) {
        List<Chantier> chantiers = clientId != null
            ? chantierService.listerParClient(clientId)
            : chantierService.lister();
        return chantiers.stream().map(ChantierResponse::from).toList();
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ChantierResponse activer(@PathVariable UUID id) {
        return ChantierResponse.from(chantierService.activer(id));
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ChantierResponse desactiver(@PathVariable UUID id) {
        return ChantierResponse.from(chantierService.desactiver(id));
    }

    @PutMapping("/{id}/chef-chantier/{utilisateurId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ChantierResponse affecterChefChantier(@PathVariable UUID id, @PathVariable UUID utilisateurId) {
        return ChantierResponse.from(chantierService.affecterChefChantier(id, utilisateurId));
    }

    @PutMapping("/{id}/salarie-responsable/{salarieId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ChantierResponse affecterSalarieResponsable(@PathVariable UUID id, @PathVariable UUID salarieId) {
        return ChantierResponse.from(chantierService.affecterSalarieResponsable(id, salarieId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        chantierService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
