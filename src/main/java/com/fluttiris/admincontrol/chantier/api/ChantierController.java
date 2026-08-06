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
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ChantierResponse> creer(@Valid @RequestBody CreateChantierRequest request) {
        Chantier chantier = chantierService.creer(
            request.nom(), request.clientId(), request.prestation(), request.adresse(), request.adresse2(),
            request.adresse3(), request.codePostal(), request.ville(), request.paysId(), request.noteInterne(),
            request.noteClient(), request.ipsAllowed(), request.dateDebut(), request.dateFinPrevue());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChantierResponse.from(chantier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ChantierResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateChantierRequest request) {
        Chantier chantier = chantierService.modifier(
            id, request.nom(), request.clientId(), request.prestation(), request.adresse(), request.adresse2(),
            request.adresse3(), request.codePostal(), request.ville(), request.paysId(), request.noteInterne(),
            request.noteClient(), request.ipsAllowed(), request.dateDebut(), request.dateFinPrevue());
        return ChantierResponse.from(chantier);
    }

    @PutMapping("/{id}/controles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ChantierResponse definirControles(@PathVariable UUID id, @RequestBody DefinirControlesRequest request) {
        Chantier chantier = chantierService.definirControles(id, request.recurrenceControles(), request.dateProchainControle());
        return ChantierResponse.from(chantier);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.chantierAccessibleParClient(#id, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isPresent() and @chantierAuthz.isAffectedToChantier(#id, @currentUser.entrepriseId().get())) or "
        + "(@currentUser.clientId().isEmpty() and @currentUser.entrepriseId().isEmpty())")
    public ChantierResponse obtenir(@PathVariable UUID id) {
        return ChantierResponse.from(chantierService.obtenir(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ChantierResponse> lister(@RequestParam(required = false) UUID clientId) {
        // Un compte Client est toujours ramené à SON propre périmètre, même s'il
        // passe un autre clientId en paramètre (cf. CurrentUser : le claim n'existe
        // que pour ce type de compte, jamais pour un compte interne).
        UUID scope = currentUser.clientId().orElse(clientId);
        List<Chantier> chantiers;
        if (currentUser.clientId().isPresent()) {
            // Un compte Client ne voit QUE les chantiers qui lui ont été explicitement
            // assignés (voir chantier_utilisateur / ChantierUtilisateurController). Sans
            // aucune assignation, il ne voit aucun chantier : l'accès est strictement
            // opt-in, jamais accordé par défaut.
            chantiers = chantierService.listerAccessiblesPourClient(currentUser.clientId().get(), currentUser.keycloakId());
        } else if (scope != null) {
            chantiers = chantierService.listerParClient(scope);
        } else if (currentUser.entrepriseId().isPresent()) {
            // Un compte Entreprise est aussi un tenant : uniquement les chantiers où
            // elle a une affectation (Principale/STT1/STT2), jamais le registre complet.
            chantiers = chantierService.listerParEntreprise(currentUser.entrepriseId().get());
        } else {
            chantiers = chantierService.lister();
        }
        return chantiers.stream().map(ChantierResponse::from).toList();
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ChantierResponse activer(@PathVariable UUID id) {
        return ChantierResponse.from(chantierService.activer(id));
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ChantierResponse desactiver(@PathVariable UUID id) {
        return ChantierResponse.from(chantierService.desactiver(id));
    }

    @PutMapping("/{id}/chef-chantier/{utilisateurId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ChantierResponse affecterChefChantier(@PathVariable UUID id, @PathVariable UUID utilisateurId) {
        return ChantierResponse.from(chantierService.affecterChefChantier(id, utilisateurId));
    }

    @PutMapping("/{id}/salarie-responsable/{salarieId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ChantierResponse affecterSalarieResponsable(@PathVariable UUID id, @PathVariable UUID salarieId) {
        return ChantierResponse.from(chantierService.affecterSalarieResponsable(id, salarieId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        chantierService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
