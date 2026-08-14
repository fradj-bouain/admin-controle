package com.fluttiris.admincontrol.auth.api;

import com.fluttiris.admincontrol.auth.api.dto.CreateUtilisateurRequest;
import com.fluttiris.admincontrol.auth.api.dto.ModifierUtilisateurRequest;
import com.fluttiris.admincontrol.auth.api.dto.UtilisateurResponse;
import com.fluttiris.admincontrol.auth.application.UtilisateurService;
import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateur;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateurRepository;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import com.fluttiris.admincontrol.common.security.ScopeAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SUPER_ADMIN gère tous les comptes. CLIENT/ENTREPRISE ont un droit
 * d'auto-gestion limité à "Mon équipe" : ils peuvent créer/lister/activer/
 * désactiver/supprimer des comptes rattachés à LEUR SEUL client/entreprise —
 * jamais choisir le rôle ou le rattachement, forcés ici quoi que contienne
 * la requête (même idiome que SalarieController.creer qui force déjà
 * entrepriseEmployeurId). CONTROLEUR n'a aucun accès à ce contrôleur.
 *
 * Pour un compte CLIENT spécifiquement, cette auto-gestion est en plus réservée
 * au profil "accès total" (voir Utilisateur.accesTousChantiers) : un "responsable
 * de chantier" n'a aucune raison de gérer les comptes du reste de l'équipe, il
 * n'est même censé voir que ses propres chantiers. Aucune restriction équivalente
 * pour ENTREPRISE, qui n'a pas cette notion à deux niveaux.
 */
@RestController
@RequestMapping("/api/v1/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final CurrentUser currentUser;
    private final ChantierUtilisateurRepository chantierUtilisateurRepository;
    private final ScopeAuthorizationService scopeAuthz;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.aAccesTousChantiers(@currentUser.keycloakId())) or "
        + "@currentUser.entrepriseId().isPresent()")
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody CreateUtilisateurRequest request) {
        Set<String> roles = request.roles();
        UUID entrepriseId = request.entrepriseId();
        UUID clientId = request.clientId();
        UUID controleTiersId = request.controleTiersId();
        // Comme le rôle et le rattachement, "accès à tous les chantiers" ne se décide
        // jamais en auto-gestion (Mon équipe) : un compte Client qui se crée lui-même
        // une équipe ne peut pas s'auto-promouvoir "accès total", il reste forcément
        // "responsable de chantier" tant qu'un SUPER_ADMIN ne l'a pas changé.
        boolean accesTousChantiers = request.accesTousChantiers();
        if (currentUser.clientId().isPresent()) {
            roles = Set.of("CLIENT");
            clientId = currentUser.clientId().get();
            entrepriseId = null;
            controleTiersId = null;
            accesTousChantiers = false;
        } else if (currentUser.entrepriseId().isPresent()) {
            roles = Set.of("ENTREPRISE");
            entrepriseId = currentUser.entrepriseId().get();
            clientId = null;
            controleTiersId = null;
            accesTousChantiers = false;
        }
        var utilisateur = utilisateurService.creer(request.username(), request.password(), request.civilite(),
            request.nom(), request.prenom(), request.email(), roles, entrepriseId, clientId, controleTiersId,
            accesTousChantiers);
        return ResponseEntity.status(HttpStatus.CREATED).body(UtilisateurResponse.from(utilisateur));
    }

    /** Un compte Client "responsable de chantier" ne gère jamais l'équipe (voir les autres
        endpoints ci-dessous), mais consulter QUI d'autre intervient reste légitime — restreint
        à ses propres collègues (voir scoperEquipePourResponsable) plutôt que bloqué en bloc :
        le registre complet du client reste réservé au profil "accès total". */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @currentUser.clientId().isPresent() or @currentUser.entrepriseId().isPresent()")
    public List<UtilisateurResponse> lister() {
        List<Utilisateur> utilisateurs;
        if (currentUser.clientId().isPresent()) {
            List<Utilisateur> equipeDuClient = utilisateurService.listerParClient(currentUser.clientId().get());
            utilisateurs = scopeAuthz.aAccesTousChantiers(currentUser.keycloakId())
                ? equipeDuClient
                : scoperEquipePourResponsable(equipeDuClient);
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

    /** Un "responsable de chantier" ne voit que les collègues avec qui il partage AU MOINS
        un chantier (voir chantier_utilisateur), plus les comptes "accès total" du client —
        toujours visibles, ce sont eux qui pilotent l'ensemble du périmètre. Un responsable
        sans aucune assignation ne voit donc que ces derniers (ou personne s'il n'y en a pas),
        cohérent avec le reste du modèle "accès strictement opt-in". */
    private List<Utilisateur> scoperEquipePourResponsable(List<Utilisateur> equipeDuClient) {
        Set<UUID> mesChantierIds = chantierUtilisateurRepository.findByUtilisateurId(currentUser.keycloakId()).stream()
            .map(ChantierUtilisateur::getChantierId)
            .collect(Collectors.toSet());
        Set<UUID> collegueIds = mesChantierIds.stream()
            .flatMap(chantierId -> chantierUtilisateurRepository.findByChantierId(chantierId).stream())
            .map(ChantierUtilisateur::getUtilisateurId)
            .collect(Collectors.toSet());
        return equipeDuClient.stream()
            .filter(u -> u.isAccesTousChantiers() || collegueIds.contains(u.getId()))
            .toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.aAccesTousChantiers(@currentUser.keycloakId()) and @scopeAuthz.utilisateurAppartientAClient(#id, @currentUser.clientId().get())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.utilisateurAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public UtilisateurResponse modifier(@PathVariable UUID id, @Valid @RequestBody ModifierUtilisateurRequest request) {
        // Même logique qu'à la création : seul un SUPER_ADMIN peut faire évoluer
        // "accès à tous les chantiers" — en auto-gestion (Mon équipe), la valeur envoyée
        // est ignorée (null = inchangé côté service).
        Boolean accesTousChantiers = currentUser.estAdmin() ? request.accesTousChantiers() : null;
        var utilisateur = utilisateurService.modifier(id, request.nom(), request.prenom(), request.email(),
            request.username(), request.password(), accesTousChantiers);
        return UtilisateurResponse.from(utilisateur);
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.aAccesTousChantiers(@currentUser.keycloakId()) and @scopeAuthz.utilisateurAppartientAClient(#id, @currentUser.clientId().get())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.utilisateurAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public UtilisateurResponse desactiver(@PathVariable UUID id) {
        return UtilisateurResponse.from(utilisateurService.desactiver(id));
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.aAccesTousChantiers(@currentUser.keycloakId()) and @scopeAuthz.utilisateurAppartientAClient(#id, @currentUser.clientId().get())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.utilisateurAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public UtilisateurResponse activer(@PathVariable UUID id) {
        return UtilisateurResponse.from(utilisateurService.activer(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.aAccesTousChantiers(@currentUser.keycloakId()) and @scopeAuthz.utilisateurAppartientAClient(#id, @currentUser.clientId().get())) or "
        + "(@currentUser.entrepriseId().isPresent() and @scopeAuthz.utilisateurAppartientAEntreprise(#id, @currentUser.entrepriseId().get()))")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        utilisateurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
