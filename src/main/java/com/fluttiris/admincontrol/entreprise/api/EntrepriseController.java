package com.fluttiris.admincontrol.entreprise.api;

import com.fluttiris.admincontrol.entreprise.api.dto.AffectationEntrepriseChantierResponse;
import com.fluttiris.admincontrol.entreprise.api.dto.CreateEntrepriseRequest;
import com.fluttiris.admincontrol.entreprise.api.dto.EntrepriseResponse;
import com.fluttiris.admincontrol.entreprise.application.AffectationEntrepriseChantierService;
import com.fluttiris.admincontrol.entreprise.application.EntrepriseService;
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
@RequestMapping("/api/v1/entreprises")
@RequiredArgsConstructor
public class EntrepriseController {

    private final EntrepriseService entrepriseService;
    private final AffectationEntrepriseChantierService affectationEntrepriseChantierService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EntrepriseResponse> creer(@Valid @RequestBody CreateEntrepriseRequest request) {
        var entreprise = entrepriseService.creer(request.raisonSociale(), request.siret(), request.adresse(),
            request.adresse2(), request.adresse3(), request.codePostal(), request.ville(), request.paysId(),
            request.corpsDeMetierId(), request.telephone(), request.telephone2(), request.telephone3(),
            request.fax(), request.email(), request.email2(), request.email3(), request.formeJuridique(),
            request.siren(), request.rcsRci(), request.tvaIntra(), request.numCotisant(),
            request.responsableSignataireAgrement(), request.commentaire());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntrepriseResponse.from(entreprise));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "@currentUser.entrepriseId().isEmpty() or @currentUser.entrepriseId().get().equals(#id)")
    public EntrepriseResponse obtenir(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.obtenir(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#id))")
    public EntrepriseResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateEntrepriseRequest request) {
        var entreprise = entrepriseService.modifier(id, request.raisonSociale(), request.siret(), request.adresse(),
            request.adresse2(), request.adresse3(), request.codePostal(), request.ville(), request.paysId(),
            request.corpsDeMetierId(), request.telephone(), request.telephone2(), request.telephone3(),
            request.fax(), request.email(), request.email2(), request.email3(), request.formeJuridique(),
            request.siren(), request.rcsRci(), request.tvaIntra(), request.numCotisant(),
            request.responsableSignataireAgrement(), request.commentaire());
        return EntrepriseResponse.from(entreprise);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EntrepriseResponse> lister() {
        // Un compte Entreprise est un tenant : il ne voit jamais le registre complet,
        // seulement sa propre fiche.
        if (currentUser.entrepriseId().isPresent()) {
            return List.of(EntrepriseResponse.from(entrepriseService.obtenir(currentUser.entrepriseId().get())));
        }
        // Un compte Client est aussi un tenant : uniquement les entreprises affectées
        // à SES chantiers, jamais le registre complet.
        if (currentUser.clientId().isPresent()) {
            return entrepriseService.listerParClient(currentUser.clientId().get()).stream()
                .map(EntrepriseResponse::from).toList();
        }
        return entrepriseService.lister().stream().map(EntrepriseResponse::from).toList();
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public EntrepriseResponse desactiver(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.desactiver(id));
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public EntrepriseResponse activer(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.activer(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        entrepriseService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/chantiers")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "@currentUser.entrepriseId().isEmpty() or @currentUser.entrepriseId().get().equals(#id)")
    public List<AffectationEntrepriseChantierResponse> listerChantiers(@PathVariable UUID id) {
        return affectationEntrepriseChantierService.listerParEntreprise(id).stream()
            .map(AffectationEntrepriseChantierResponse::from)
            .toList();
    }
}
