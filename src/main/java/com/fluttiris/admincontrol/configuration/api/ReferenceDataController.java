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
}
