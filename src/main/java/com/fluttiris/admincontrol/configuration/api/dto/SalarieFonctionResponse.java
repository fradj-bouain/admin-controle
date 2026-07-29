package com.fluttiris.admincontrol.configuration.api.dto;

import com.fluttiris.admincontrol.configuration.domain.SalarieFonction;

import java.util.UUID;

public record SalarieFonctionResponse(UUID id, String libelle) {
    public static SalarieFonctionResponse from(SalarieFonction f) {
        return new SalarieFonctionResponse(f.getId(), f.getLibelle());
    }
}
