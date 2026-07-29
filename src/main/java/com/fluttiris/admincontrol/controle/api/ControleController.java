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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ControleResponse> creer(@Valid @RequestBody CreateControleRequest request) {
        var controle = controleService.creer(request.chantierId(), currentUser.keycloakId(),
            request.dateControle(), request.remarques(), request.controleTiersId(), request.dateFin(), request.termine());
        return ResponseEntity.status(HttpStatus.CREATED).body(ControleResponse.from(controle));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ControleResponse> lister(@RequestParam UUID chantierId) {
        return controleService.listerParChantier(chantierId).stream().map(ControleResponse::from).toList();
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
