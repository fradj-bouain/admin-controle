package com.fluttiris.admincontrol.document.api.dto;

import com.fluttiris.admincontrol.document.domain.DocumentEtat;

import java.util.UUID;

public record DocumentEtatResponse(UUID id, String titre, boolean parDefaut, boolean dateExpiree, boolean valideLeDocument) {
    public static DocumentEtatResponse from(DocumentEtat e) {
        return new DocumentEtatResponse(e.getId(), e.getTitre(), e.isParDefaut(), e.isDateExpiree(), e.isValideLeDocument());
    }
}
