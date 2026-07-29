package com.fluttiris.admincontrol.entreprise.api.dto;

import com.fluttiris.admincontrol.entreprise.domain.RoleEntreprise;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AffecterEntrepriseRequest(
    @NotNull UUID entrepriseId,
    @NotNull RoleEntreprise role,
    /** null pour une entreprise PRINCIPALE ; obligatoire (affectation du parent sur ce chantier) pour STT1/STT2. */
    UUID affectationParenteId
) {
}
