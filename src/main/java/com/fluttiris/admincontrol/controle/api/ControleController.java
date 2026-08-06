package com.fluttiris.admincontrol.controle.api;

import com.fluttiris.admincontrol.controle.api.dto.AjouterControleSalarieRequest;
import com.fluttiris.admincontrol.controle.api.dto.ControleResponse;
import com.fluttiris.admincontrol.controle.api.dto.ControleSalarieResponse;
import com.fluttiris.admincontrol.controle.api.dto.CreateControleRequest;
import com.fluttiris.admincontrol.controle.api.dto.CreateRapportRequest;
import com.fluttiris.admincontrol.controle.api.dto.ModifierControleRequest;
import com.fluttiris.admincontrol.controle.api.dto.ModifierControleSalarieRequest;
import com.fluttiris.admincontrol.controle.api.dto.ModifierRapportRequest;
import com.fluttiris.admincontrol.controle.api.dto.RapportControleResponse;
import com.fluttiris.admincontrol.controle.application.ControleService;
import com.fluttiris.admincontrol.controle.domain.Controle;
import com.fluttiris.admincontrol.controle.domain.RapportControle;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import com.fluttiris.admincontrol.common.security.ScopeAuthorizationService;
import com.fluttiris.admincontrol.entreprise.application.ChantierAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/controles")
@RequiredArgsConstructor
public class ControleController {

    private final ControleService controleService;
    private final CurrentUser currentUser;
    private final ScopeAuthorizationService scopeAuthz;
    private final ChantierAuthorizationService chantierAuthz;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @currentUser.controleTiersId().isPresent()")
    public ResponseEntity<ControleResponse> creer(@Valid @RequestBody CreateControleRequest request) {
        // Un compte Contrôleur ne peut créer un contrôle que pour SON organisme,
        // même s'il passe un autre controleTiersId dans la requête.
        UUID controleTiersId = currentUser.controleTiersId().orElse(request.controleTiersId());
        var controle = controleService.creer(request.chantierId(), currentUser.keycloakId(),
            request.dateControle(), request.remarques(), controleTiersId, request.dateFin(), request.termine());
        return ResponseEntity.status(HttpStatus.CREATED).body(ControleResponse.from(controle));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ControleResponse> lister(@RequestParam(required = false) UUID chantierId) {
        List<Controle> controles;
        if (chantierId != null) {
            // Un chantierId explicite ne doit renvoyer des contrôles que si l'appelant a
            // réellement accès à ce chantier (sinon un Client/Entreprise pourrait sonder
            // les contrôles de n'importe quel chantier en devinant son id).
            if (!chantierAccessible(chantierId)) {
                return List.of();
            }
            controles = controleService.listerParChantier(chantierId);
            // Un compte Contrôleur ne voit que les contrôles de SON organisme sur ce chantier.
            if (currentUser.controleTiersId().isPresent()) {
                UUID scope = currentUser.controleTiersId().get();
                controles = controles.stream().filter(c -> scope.equals(c.getControleTiersId())).toList();
            }
        } else if (currentUser.controleTiersId().isPresent()) {
            // Sans chantier précisé : un compte Contrôleur voit tous les contrôles de SON organisme (dashboard).
            controles = controleService.listerParControleTiers(currentUser.controleTiersId().get());
        } else if (currentUser.clientId().isPresent()) {
            // Un compte Client est un tenant : uniquement les contrôles des chantiers qui
            // lui ont été explicitement assignés — aucun chantier assigné = aucun contrôle visible.
            controles = controleService.listerParClient(currentUser.clientId().get(), currentUser.keycloakId());
        } else if (currentUser.entrepriseId().isPresent()) {
            // Un compte Entreprise est aussi un tenant : uniquement les contrôles des
            // chantiers où elle a une affectation.
            controles = controleService.listerParEntreprise(currentUser.entrepriseId().get());
        } else {
            controles = controleService.lister();
        }
        return controles.stream().map(ControleResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.controleTiersId().isPresent() and @scopeAuthz.controleAppartientAOrganisme(#id, @currentUser.controleTiersId().get())) or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.controleAppartientAuClient(#id, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.controleAccessibleParEntreprise(#id, @currentUser.entrepriseId().get()))")
    public ControleResponse obtenir(@PathVariable UUID id) {
        return ControleResponse.from(controleService.obtenir(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @currentUser.controleTiersId().isPresent()")
    public ControleResponse modifier(@PathVariable UUID id, @Valid @RequestBody ModifierControleRequest request) {
        UUID controleTiersId = currentUser.controleTiersId().orElse(request.controleTiersId());
        var controle = controleService.modifier(id, request.dateControle(), request.remarques(), controleTiersId,
            request.dateFin(), request.termine());
        return ControleResponse.from(controle);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        controleService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // --- Checklist salariés (accord/refus individuel constaté sur site) ---

    @PostMapping("/{id}/salaries")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @currentUser.controleTiersId().isPresent()")
    public ResponseEntity<ControleSalarieResponse> ajouterSalarie(@PathVariable UUID id, @Valid @RequestBody AjouterControleSalarieRequest request) {
        var entree = controleService.ajouterSalarieAuControle(id, request.salarieId(), request.entrepriseId(),
            request.accorde(), request.actionCorrectiveId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ControleSalarieResponse.from(entree));
    }

    @GetMapping("/{id}/salaries")
    @PreAuthorize("isAuthenticated()")
    public List<ControleSalarieResponse> listerSalaries(@PathVariable UUID id) {
        return controleService.listerSalariesDuControle(id).stream().map(ControleSalarieResponse::from).toList();
    }

    @PutMapping("/{controleId}/salaries/{entreeId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @currentUser.controleTiersId().isPresent()")
    public ControleSalarieResponse modifierSalarie(@PathVariable UUID controleId, @PathVariable UUID entreeId,
                                                     @Valid @RequestBody ModifierControleSalarieRequest request) {
        var entree = controleService.modifierEntreeControleSalarie(entreeId, request.accorde(), request.actionCorrectiveId());
        return ControleSalarieResponse.from(entree);
    }

    @DeleteMapping("/{controleId}/salaries/{entreeId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @currentUser.controleTiersId().isPresent()")
    public ResponseEntity<Void> supprimerSalarie(@PathVariable UUID controleId, @PathVariable UUID entreeId) {
        controleService.supprimerEntreeControleSalarie(entreeId);
        return ResponseEntity.noContent().build();
    }

    // --- Rapports ---

    @PostMapping("/rapports")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RapportControleResponse> genererRapport(@Valid @RequestBody CreateRapportRequest request) {
        var rapport = controleService.genererRapport(request.controleId(), request.nbNouvellesEntreprises(),
            request.nbNouveauxSalaries(), request.nbSalariesDetaches(), request.responsableUtilisateurId());
        return ResponseEntity.status(HttpStatus.CREATED).body(controleService.toResponse(rapport));
    }

    @GetMapping("/rapports")
    @PreAuthorize("isAuthenticated()")
    public List<RapportControleResponse> listerRapports(@RequestParam(required = false) UUID chantierId) {
        List<RapportControle> rapports;
        if (chantierId != null) {
            if (!chantierAccessible(chantierId)) {
                return List.of();
            }
            rapports = controleService.listerRapportsParChantier(chantierId);
        } else if (currentUser.clientId().isPresent()) {
            rapports = controleService.listerRapportsParClient(currentUser.clientId().get(), currentUser.keycloakId());
        } else if (currentUser.entrepriseId().isPresent()) {
            rapports = controleService.listerRapportsParEntreprise(currentUser.entrepriseId().get());
        } else {
            rapports = controleService.listerRapports();
        }
        // Un compte Contrôleur ne voit que les rapports issus des contrôles de SON organisme.
        if (currentUser.controleTiersId().isPresent()) {
            rapports = controleService.filtrerRapportsParControleTiers(rapports, currentUser.controleTiersId().get());
        }
        Map<UUID, UUID> chantierIdParRapport = controleService.resoudreChantierIdParRapport(rapports);
        return rapports.stream().map(r -> RapportControleResponse.from(r, chantierIdParRapport.get(r.getId()))).toList();
    }

    @GetMapping("/rapports/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.controleTiersId().isPresent() and @scopeAuthz.rapportAppartientAOrganisme(#id, @currentUser.controleTiersId().get())) or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.rapportAppartientAuClient(#id, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.rapportAccessibleParEntreprise(#id, @currentUser.entrepriseId().get()))")
    public RapportControleResponse obtenirRapport(@PathVariable UUID id) {
        return controleService.toResponse(controleService.obtenirRapport(id));
    }

    @PutMapping("/rapports/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RapportControleResponse modifierRapport(@PathVariable UUID id, @Valid @RequestBody ModifierRapportRequest request) {
        var rapport = controleService.modifierRapport(id, request.nbNouvellesEntreprises(), request.nbNouveauxSalaries(),
            request.nbSalariesDetaches(), request.responsableUtilisateurId());
        return controleService.toResponse(rapport);
    }

    @DeleteMapping("/rapports/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimerRapport(@PathVariable UUID id) {
        controleService.supprimerRapport(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rapports/{id}/envoyer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RapportControleResponse envoyerRapport(@PathVariable UUID id) {
        return controleService.toResponse(controleService.envoyerRapport(id));
    }

    @PostMapping("/rapports/{id}/renvoyer-email")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> renvoyerRapportParEmail(@PathVariable UUID id) {
        controleService.renvoyerRapportParEmail(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Un Client ou une Entreprise ne peuvent lister les contrôles/rapports d'un chantierId
     * explicite que si ce chantier appartient réellement à leur périmètre (pour un Client,
     * en tenant compte d'une éventuelle restriction à certains chantiers précis — voir
     * ChantierService.listerAccessiblesPourClient). Un Contrôleur ou un compte interne
     * restent, eux, non restreints à ce niveau (le filtrage par organisme s'applique
     * séparément, après coup, sur la liste de contrôles/rapports elle-même).
     */
    private boolean chantierAccessible(UUID chantierId) {
        if (currentUser.clientId().isPresent()) {
            return scopeAuthz.chantierAccessibleParClient(chantierId, currentUser.clientId().get(), currentUser.keycloakId());
        }
        if (currentUser.entrepriseId().isPresent()) {
            return chantierAuthz.isAffectedToChantier(chantierId, currentUser.entrepriseId().get());
        }
        return true;
    }
}
