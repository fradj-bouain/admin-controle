package com.fluttiris.admincontrol.document.api.dto;

import com.fluttiris.admincontrol.document.domain.CibleDocument;
import com.fluttiris.admincontrol.document.domain.FormatDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTypeDocumentRequest(
    @NotBlank String libelle,
    @NotNull CibleDocument cible,
    boolean obligatoire,
    @NotNull FormatDocument format,
    UUID corpsDeMetierId,
    UUID paysId,
    String zoneRequise,
    boolean dateDebutValiditeRequise,
    boolean dateFinValiditeRequise,
    int nbJoursRelanceAvant,
    int nbJoursRecurrence,
    boolean retireAccordAcces
) {
}
