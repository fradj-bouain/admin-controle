package com.fluttiris.admincontrol.document.api;

import com.fluttiris.admincontrol.document.api.dto.CreateTypeDocumentRequest;
import com.fluttiris.admincontrol.document.api.dto.TypeDocumentResponse;
import com.fluttiris.admincontrol.document.application.TypeDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/types-document")
@RequiredArgsConstructor
public class TypeDocumentController {

    private final TypeDocumentService typeDocumentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TypeDocumentResponse> creer(@Valid @RequestBody CreateTypeDocumentRequest request) {
        var type = typeDocumentService.creer(request.libelle(), request.cible(), request.obligatoire(),
            request.format(), request.corpsDeMetierId(), request.paysId(), request.dateDebutValiditeRequise(),
            request.dateFinValiditeRequise(), request.nbJoursRelanceAvant(), request.nbJoursRecurrence(),
            request.retireAccordAcces());
        return ResponseEntity.status(HttpStatus.CREATED).body(TypeDocumentResponse.from(type));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TypeDocumentResponse> lister() {
        return typeDocumentService.lister().stream().map(TypeDocumentResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TypeDocumentResponse obtenir(@PathVariable UUID id) {
        return TypeDocumentResponse.from(typeDocumentService.obtenir(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public TypeDocumentResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateTypeDocumentRequest request) {
        var type = typeDocumentService.modifier(id, request.libelle(), request.cible(), request.obligatoire(),
            request.format(), request.corpsDeMetierId(), request.paysId(), request.dateDebutValiditeRequise(),
            request.dateFinValiditeRequise(), request.nbJoursRelanceAvant(), request.nbJoursRecurrence(),
            request.retireAccordAcces());
        return TypeDocumentResponse.from(type);
    }
}
