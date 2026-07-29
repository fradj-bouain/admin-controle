package com.fluttiris.admincontrol.client.api;

import com.fluttiris.admincontrol.client.api.dto.ClientResponse;
import com.fluttiris.admincontrol.client.api.dto.CreateClientRequest;
import com.fluttiris.admincontrol.client.application.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ClientResponse> creer(@Valid @RequestBody CreateClientRequest request) {
        var client = clientService.creer(request.raisonSociale(), request.adresse(), request.adresse2(),
            request.adresse3(), request.codePostal(), request.ville(), request.paysId(), request.telephone(),
            request.telephone2(), request.telephone3(), request.fax(), request.email(), request.email2(),
            request.email3(), request.formeJuridique(), request.siren(), request.siret(), request.rcsRci(),
            request.tvaIntra(), request.numCotisant(), request.responsableSignataireAgrement());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ClientResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateClientRequest request) {
        var client = clientService.modifier(id, request.raisonSociale(), request.adresse(), request.adresse2(),
            request.adresse3(), request.codePostal(), request.ville(), request.paysId(), request.telephone(),
            request.telephone2(), request.telephone3(), request.fax(), request.email(), request.email2(),
            request.email3(), request.formeJuridique(), request.siren(), request.siret(), request.rcsRci(),
            request.tvaIntra(), request.numCotisant(), request.responsableSignataireAgrement());
        return ClientResponse.from(client);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ClientResponse obtenir(@PathVariable UUID id) {
        return ClientResponse.from(clientService.obtenir(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ClientResponse> lister() {
        return clientService.lister().stream().map(ClientResponse::from).toList();
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ClientResponse desactiver(@PathVariable UUID id) {
        return ClientResponse.from(clientService.desactiver(id));
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ClientResponse activer(@PathVariable UUID id) {
        return ClientResponse.from(clientService.activer(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        clientService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
