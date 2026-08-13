package com.fluttiris.admincontrol.entreprise.api;

import com.fluttiris.admincontrol.entreprise.api.dto.AffectationEntrepriseChantierResponse;
import com.fluttiris.admincontrol.entreprise.api.dto.CreateEntrepriseRequest;
import com.fluttiris.admincontrol.entreprise.api.dto.EntrepriseResponse;
import com.fluttiris.admincontrol.entreprise.application.AffectationEntrepriseChantierService;
import com.fluttiris.admincontrol.entreprise.application.EntrepriseService;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
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
        + "(@currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#id)) or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.entrepriseAccessibleParClient(#id, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isEmpty() and @currentUser.clientId().isEmpty())")
    public EntrepriseResponse obtenir(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.obtenir(id));
    }

    // Modification de la fiche entreprise : réservée au SUPER_ADMIN. L'Entreprise pouvait
    // auparavant modifier sa propre fiche (coordonnées + infos légales) ; retiré à la demande
    // du client réel du projet, qui veut que l'Entreprise reste en pur consultation sur ses
    // propres informations comme sur celles de ses salariés (voir SalarieController).
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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
            return avecChantierActuel(List.of(entrepriseService.obtenir(currentUser.entrepriseId().get())));
        }
        // Un compte Client est aussi un tenant : uniquement les entreprises affectées aux
        // chantiers qui lui ont été explicitement assignés, jamais le registre complet —
        // aucun chantier assigné = aucune entreprise visible.
        if (currentUser.clientId().isPresent()) {
            return avecChantierActuel(
                entrepriseService.listerParClient(currentUser.clientId().get(), currentUser.keycloakId()));
        }
        return avecChantierActuel(entrepriseService.lister());
    }

    private List<EntrepriseResponse> avecChantierActuel(List<Entreprise> entreprises) {
        var ids = entreprises.stream().map(Entreprise::getId).toList();
        var chantierActuelParEntreprise = entrepriseService.chantierActuelParEntreprise(ids);
        var rangActuelParEntreprise = entrepriseService.rangActuelParEntreprise(ids);
        return entreprises.stream()
            .map(e -> EntrepriseResponse.from(e, chantierActuelParEntreprise.get(e.getId()), rangActuelParEntreprise.get(e.getId())))
            .toList();
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
        + "(@currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#id)) or "
        + "@currentUser.entrepriseId().isEmpty()")
    public List<AffectationEntrepriseChantierResponse> listerChantiers(@PathVariable UUID id) {
        return affectationEntrepriseChantierService.listerParEntreprise(id).stream()
            .map(AffectationEntrepriseChantierResponse::from)
            .toList();
    }
}
