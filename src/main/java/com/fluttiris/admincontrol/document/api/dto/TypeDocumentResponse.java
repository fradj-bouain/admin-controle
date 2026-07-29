package com.fluttiris.admincontrol.document.api.dto;

import com.fluttiris.admincontrol.document.domain.CibleDocument;
import com.fluttiris.admincontrol.document.domain.FormatDocument;
import com.fluttiris.admincontrol.document.domain.TypeDocument;

import java.util.UUID;

public record TypeDocumentResponse(
    UUID id,
    String libelle,
    CibleDocument cible,
    boolean obligatoire,
    FormatDocument format,
    UUID corpsDeMetierId,
    UUID paysId,
    boolean dateDebutValiditeRequise,
    boolean dateFinValiditeRequise,
    int nbJoursRelanceAvant,
    int nbJoursRecurrence,
    boolean retireAccordAcces
) {
    public static TypeDocumentResponse from(TypeDocument t) {
        return new TypeDocumentResponse(t.getId(), t.getLibelle(), t.getCible(), t.isObligatoire(),
            t.getFormat(), t.getCorpsDeMetierId(), t.getPaysId(), t.isDateDebutValiditeRequise(),
            t.isDateFinValiditeRequise(), t.getNbJoursRelanceAvant(), t.getNbJoursRecurrence(),
            t.isRetireAccordAcces());
    }
}
