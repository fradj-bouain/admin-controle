package com.fluttiris.admincontrol.configuration.api.dto;

import com.fluttiris.admincontrol.configuration.domain.Pays;

import java.util.UUID;

public record PaysResponse(UUID id, String codeIso, String nom, String zone) {
    public static PaysResponse from(Pays p) {
        return new PaysResponse(p.getId(), p.getCodeIso(), p.getNom(), p.getZone());
    }
}
