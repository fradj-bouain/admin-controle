package com.fluttiris.admincontrol.chantier.api;

import com.fluttiris.admincontrol.chantier.api.dto.AccorderAccesRequest;
import com.fluttiris.admincontrol.chantier.api.dto.ChantierUtilisateurResponse;
import com.fluttiris.admincontrol.chantier.application.ChantierUtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chantiers/{chantierId}/utilisateurs")
@RequiredArgsConstructor
public class ChantierUtilisateurController {

    private final ChantierUtilisateurService chantierUtilisateurService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ChantierUtilisateurResponse> accorder(
        @PathVariable UUID chantierId, @Valid @RequestBody AccorderAccesRequest request) {
        var acces = chantierUtilisateurService.accorder(chantierId, request.utilisateurId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChantierUtilisateurResponse.from(acces));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<ChantierUtilisateurResponse> lister(@PathVariable UUID chantierId) {
        return chantierUtilisateurService.lister(chantierId).stream().map(ChantierUtilisateurResponse::from).toList();
    }

    @DeleteMapping("/{utilisateurId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> revoquer(@PathVariable UUID chantierId, @PathVariable UUID utilisateurId) {
        chantierUtilisateurService.revoquer(chantierId, utilisateurId);
        return ResponseEntity.noContent().build();
    }
}
