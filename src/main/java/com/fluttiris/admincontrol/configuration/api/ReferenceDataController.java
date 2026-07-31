package com.fluttiris.admincontrol.configuration.api;

import com.fluttiris.admincontrol.configuration.api.dto.ControleTiersResponse;
import com.fluttiris.admincontrol.configuration.api.dto.CorpsDeMetierResponse;
import com.fluttiris.admincontrol.configuration.api.dto.CreateControleTiersRequest;
import com.fluttiris.admincontrol.configuration.api.dto.CreateCorpsDeMetierRequest;
import com.fluttiris.admincontrol.configuration.api.dto.CreatePaysRequest;
import com.fluttiris.admincontrol.configuration.api.dto.CreateSalarieFonctionRequest;
import com.fluttiris.admincontrol.configuration.api.dto.CreateTypeContratSalarieRequest;
import com.fluttiris.admincontrol.configuration.api.dto.CreateTypeSalarieRequest;
import com.fluttiris.admincontrol.configuration.api.dto.PaysResponse;
import com.fluttiris.admincontrol.configuration.api.dto.SalarieFonctionResponse;
import com.fluttiris.admincontrol.configuration.api.dto.TypeContratSalarieResponse;
import com.fluttiris.admincontrol.configuration.api.dto.TypeSalarieResponse;
import com.fluttiris.admincontrol.configuration.application.ReferenceDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/configuration")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @PostMapping("/pays")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PaysResponse> creerPays(@Valid @RequestBody CreatePaysRequest request) {
        var pays = referenceDataService.creerPays(request.codeIso(), request.nom(), request.zone());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaysResponse.from(pays));
    }

    @GetMapping("/pays")
    @PreAuthorize("isAuthenticated()")
    public List<PaysResponse> listerPays() {
        return referenceDataService.listerPays().stream().map(PaysResponse::from).toList();
    }

    @PutMapping("/pays/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public PaysResponse modifierPays(@PathVariable UUID id, @Valid @RequestBody CreatePaysRequest request) {
        return PaysResponse.from(referenceDataService.modifierPays(id, request.codeIso(), request.nom(), request.zone()));
    }

    @PostMapping("/corps-de-metier")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<CorpsDeMetierResponse> creerCorpsDeMetier(@Valid @RequestBody CreateCorpsDeMetierRequest request) {
        var corps = referenceDataService.creerCorpsDeMetier(request.libelle());
        return ResponseEntity.status(HttpStatus.CREATED).body(CorpsDeMetierResponse.from(corps));
    }

    @GetMapping("/corps-de-metier")
    @PreAuthorize("isAuthenticated()")
    public List<CorpsDeMetierResponse> listerCorpsDeMetier() {
        return referenceDataService.listerCorpsDeMetier().stream().map(CorpsDeMetierResponse::from).toList();
    }

    @PutMapping("/corps-de-metier/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public CorpsDeMetierResponse modifierCorpsDeMetier(@PathVariable UUID id, @Valid @RequestBody CreateCorpsDeMetierRequest request) {
        return CorpsDeMetierResponse.from(referenceDataService.modifierCorpsDeMetier(id, request.libelle()));
    }

    @PostMapping("/types-salarie")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TypeSalarieResponse> creerTypeSalarie(@Valid @RequestBody CreateTypeSalarieRequest request) {
        var type = referenceDataService.creerTypeSalarie(request.code(), request.libelle());
        return ResponseEntity.status(HttpStatus.CREATED).body(TypeSalarieResponse.from(type));
    }

    @GetMapping("/types-salarie")
    @PreAuthorize("isAuthenticated()")
    public List<TypeSalarieResponse> listerTypeSalarie() {
        return referenceDataService.listerTypeSalarie().stream().map(TypeSalarieResponse::from).toList();
    }

    @PutMapping("/types-salarie/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public TypeSalarieResponse modifierTypeSalarie(@PathVariable UUID id, @Valid @RequestBody CreateTypeSalarieRequest request) {
        return TypeSalarieResponse.from(referenceDataService.modifierTypeSalarie(id, request.code(), request.libelle()));
    }

    @PostMapping("/types-contrat-salarie")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TypeContratSalarieResponse> creerTypeContratSalarie(@Valid @RequestBody CreateTypeContratSalarieRequest request) {
        var type = referenceDataService.creerTypeContratSalarie(request.code(), request.libelle());
        return ResponseEntity.status(HttpStatus.CREATED).body(TypeContratSalarieResponse.from(type));
    }

    @GetMapping("/types-contrat-salarie")
    @PreAuthorize("isAuthenticated()")
    public List<TypeContratSalarieResponse> listerTypeContratSalarie() {
        return referenceDataService.listerTypeContratSalarie().stream().map(TypeContratSalarieResponse::from).toList();
    }

    @PutMapping("/types-contrat-salarie/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public TypeContratSalarieResponse modifierTypeContratSalarie(@PathVariable UUID id, @Valid @RequestBody CreateTypeContratSalarieRequest request) {
        return TypeContratSalarieResponse.from(referenceDataService.modifierTypeContratSalarie(id, request.code(), request.libelle()));
    }

    @PostMapping("/salarie-fonctions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<SalarieFonctionResponse> creerSalarieFonction(@Valid @RequestBody CreateSalarieFonctionRequest request) {
        var fonction = referenceDataService.creerSalarieFonction(request.libelle());
        return ResponseEntity.status(HttpStatus.CREATED).body(SalarieFonctionResponse.from(fonction));
    }

    @GetMapping("/salarie-fonctions")
    @PreAuthorize("isAuthenticated()")
    public List<SalarieFonctionResponse> listerSalarieFonction() {
        return referenceDataService.listerSalarieFonction().stream().map(SalarieFonctionResponse::from).toList();
    }

    @PutMapping("/salarie-fonctions/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public SalarieFonctionResponse modifierSalarieFonction(@PathVariable UUID id, @Valid @RequestBody CreateSalarieFonctionRequest request) {
        return SalarieFonctionResponse.from(referenceDataService.modifierSalarieFonction(id, request.libelle()));
    }

    @PostMapping("/controle-tiers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ControleTiersResponse> creerControleTiers(@Valid @RequestBody CreateControleTiersRequest request) {
        var tiers = referenceDataService.creerControleTiers(request.nom());
        return ResponseEntity.status(HttpStatus.CREATED).body(ControleTiersResponse.from(tiers));
    }

    @GetMapping("/controle-tiers")
    @PreAuthorize("isAuthenticated()")
    public List<ControleTiersResponse> listerControleTiers() {
        return referenceDataService.listerControleTiers().stream().map(ControleTiersResponse::from).toList();
    }

    @PutMapping("/controle-tiers/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ControleTiersResponse modifierControleTiers(@PathVariable UUID id, @Valid @RequestBody CreateControleTiersRequest request) {
        return ControleTiersResponse.from(referenceDataService.modifierControleTiers(id, request.nom()));
    }
}
