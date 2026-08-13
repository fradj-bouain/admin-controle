package com.fluttiris.admincontrol.auth.api;

import com.fluttiris.admincontrol.auth.api.dto.CreateUtilisateurRequest;
import com.fluttiris.admincontrol.auth.api.dto.ModifierUtilisateurRequest;
import com.fluttiris.admincontrol.auth.api.dto.UtilisateurResponse;
import com.fluttiris.admincontrol.auth.application.UtilisateurService;
import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateurRepository;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SUPER_ADMIN gère tous les comptes. CLIENT/ENTREPRISE ont un droit
 * d'auto-gestion limité à "Mon équipe" : ils peuvent créer/lister/activer/
 * désactiver/supprimer des comptes rattachés à LEUR SEUL client/entreprise —
 * jamais choisir le rôle ou le rattachement, forcés ici quoi que contienne
 * la requête (même idiome que SalarieController.creer qui force déjà
 * entrepriseEmployeurId). CONTROLEUR n'a aucun accès à ce contrôleur.
 */
@RestController
@RequestMapping("/api/v1/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final CurrentUser currentUser;
    private final ChantierUtilisateurRepository chantierUtilisateurRepository;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @currentUser.clientId().isPresent() or @currentUser.entrepriseId().isPresent()")
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody CreateUtilisateurRequest request) {
        Set<String> roles = request.roles();
        UUID entrepriseId = request.entrepriseId();
        UUID clientId = request.clientId();
        UUID controleTiersId = request.controleTiersId();
        if (currentUser.clientId().isPresent()) {
            roles = Set.of("CLIENT");
            clientId = currentUser.clientId().get();
            entrepriseId = null;
            controleTiersId = null;
        } else if (currentUser.entrepriseId().isPresent()) {
            roles = Set.of("ENTREPRISE");
            entrepriseId = currentUser.entrepriseId().get();
            clientId = null;
            controleTiersId = null;
        }
        var utilisateur = utilisateurService.creer(request.username(), request.password(), request.civilite(),
            request.nom(), request.prenom(), request.email(), roles, entrepriseId, clientId, controleTiersId);
        return ResponseEntity.status(HttpStatus.CREATED).body(UtilisateurResponse.from(utilisateur));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @currentUser.clientId().isPresent() or @currentUser.entrepriseId().isPresent()")
    public List<UtilisateurResponse> lister() {
        List<Utilisateur> utilisateurs;
        if (currentUser.clientId().isPresent()) {
            utilisateurs = utilisateurService.listerParClient(currentUser.clientId().get());
        } else if (currentUser.entrepriseId().isPresent()) {
            utilisateurs = utilisateurService.listerParEntreprise(currentUser.entrepriseId().get());
        } else {
            utilisateurs = utilisateurService.lister();
        }
        // Compte affiché uniquement pour les comptes CLIENT (voir audit UX) : le
        // nombre de chantiers auxquels chacun a été explicitement assigné, pour que
        // "aucun accès" (0 assignation = aucune visibilité, voir ScopeAuthorizationService)
        // soit visible sur cette liste plutôt que découvert en silence par l'utilisateur.
        return utilisateurs.stream()
            .map(u -> UtilisateurResponse.from(u, u.getClientId() != null
                ? chantierUtilisateurRepository.findByUtilisateurId(u.getId()).size()
                : 0))
            .toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.utilisateurAppartientAClient(#id, @currentUser.clientId().get())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.utilisateurAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public UtilisateurResponse modifier(@PathVariable UUID id, @Valid @RequestBody ModifierUtilisateurRequest request) {
        var utilisateur = utilisateurService.modifier(id, request.nom(), request.prenom(), request.email(),
            request.username(), request.password());
        return UtilisateurResponse.from(utilisateur);
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.utilisateurAppartientAClient(#id, @currentUser.clientId().get())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.utilisateurAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public UtilisateurResponse desactiver(@PathVariable UUID id) {
        return UtilisateurResponse.from(utilisateurService.desactiver(id));
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.utilisateurAppartientAClient(#id, @currentUser.clientId().get())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.utilisateurAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public UtilisateurResponse activer(@PathVariable UUID id) {
        return UtilisateurResponse.from(utilisateurService.activer(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.utilisateurAppartientAClient(#id, @currentUser.clientId().get())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.utilisateurAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        utilisateurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
