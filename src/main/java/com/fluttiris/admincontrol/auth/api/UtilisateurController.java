package com.fluttiris.admincontrol.auth.api;

import com.fluttiris.admincontrol.auth.api.dto.CreateUtilisateurRequest;
import com.fluttiris.admincontrol.auth.api.dto.UtilisateurResponse;
import com.fluttiris.admincontrol.auth.application.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @PostMapping
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody CreateUtilisateurRequest request) {
        var utilisateur = utilisateurService.creer(request.username(), request.password(), request.civilite(),
            request.nom(), request.prenom(), request.email(), request.roles(), request.entrepriseId(), request.clientId(),
            request.controleTiersId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UtilisateurResponse.from(utilisateur));
    }

    @GetMapping
    public List<UtilisateurResponse> lister() {
        return utilisateurService.lister().stream().map(UtilisateurResponse::from).toList();
    }
}
