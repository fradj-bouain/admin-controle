package com.fluttiris.admincontrol.entreprise.api;

import com.fluttiris.admincontrol.entreprise.api.dto.AffectationEntrepriseChantierResponse;
import com.fluttiris.admincontrol.entreprise.api.dto.CreateEntrepriseRequest;
import com.fluttiris.admincontrol.entreprise.api.dto.EntrepriseResponse;
import com.fluttiris.admincontrol.entreprise.application.AffectationEntrepriseChantierService;
import com.fluttiris.admincontrol.entreprise.application.EntrepriseService;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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
    @PreAuthorize("isAuthenticated()")
    public EntrepriseResponse obtenir(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.obtenir(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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
        return entrepriseService.lister().stream().map(EntrepriseResponse::from).toList();
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public EntrepriseResponse desactiver(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.desactiver(id));
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public EntrepriseResponse activer(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.activer(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        entrepriseService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/chantiers")
    @PreAuthorize("isAuthenticated()")
    public List<AffectationEntrepriseChantierResponse> listerChantiers(@PathVariable UUID id) {
        return affectationEntrepriseChantierService.listerParEntreprise(id).stream()
            .map(AffectationEntrepriseChantierResponse::from)
            .toList();
    }
}
