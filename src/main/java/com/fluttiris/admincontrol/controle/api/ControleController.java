package com.fluttiris.admincontrol.controle.api;

import com.fluttiris.admincontrol.controle.api.dto.ControleResponse;
import com.fluttiris.admincontrol.controle.api.dto.CreateControleRequest;
import com.fluttiris.admincontrol.controle.api.dto.CreateRapportRequest;
import com.fluttiris.admincontrol.controle.api.dto.RapportControleResponse;
import com.fluttiris.admincontrol.controle.application.ControleService;
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
@RequestMapping("/api/v1/controles")
@RequiredArgsConstructor
public class ControleController {

    private final ControleService controleService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @currentUser.controleTiersId().isPresent()")
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
    public List<ControleResponse> lister(@RequestParam UUID chantierId) {
        var controles = controleService.listerParChantier(chantierId);
        // Un compte Contrôleur ne voit que les contrôles de SON organisme sur ce chantier.
        if (currentUser.controleTiersId().isPresent()) {
            UUID scope = currentUser.controleTiersId().get();
            controles = controles.stream().filter(c -> scope.equals(c.getControleTiersId())).toList();
        }
        return controles.stream().map(ControleResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        controleService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rapports")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<RapportControleResponse> genererRapport(@Valid @RequestBody CreateRapportRequest request) {
        var rapport = controleService.genererRapport(request.controleId(), request.nbSalariesControles(),
            request.nbAccords(), request.nbRefus(), request.nbNouvellesEntreprises(), request.nbNouveauxSalaries(),
            request.nbEntreprises(), request.nbSalariesDetaches(), request.responsableUtilisateurId());
        return ResponseEntity.status(HttpStatus.CREATED).body(RapportControleResponse.from(rapport));
    }

    @GetMapping("/rapports")
    @PreAuthorize("isAuthenticated()")
    public List<RapportControleResponse> listerRapports() {
        return controleService.listerRapports().stream().map(RapportControleResponse::from).toList();
    }

    @PostMapping("/rapports/{id}/envoyer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public RapportControleResponse envoyerRapport(@PathVariable UUID id) {
        return RapportControleResponse.from(controleService.envoyerRapport(id));
    }
}
