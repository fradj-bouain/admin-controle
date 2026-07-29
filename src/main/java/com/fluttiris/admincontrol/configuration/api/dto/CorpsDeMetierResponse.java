package com.fluttiris.admincontrol.configuration.api.dto;

import com.fluttiris.admincontrol.configuration.domain.CorpsDeMetier;

import java.util.UUID;

public record CorpsDeMetierResponse(UUID id, String libelle) {
    public static CorpsDeMetierResponse from(CorpsDeMetier c) {
        return new CorpsDeMetierResponse(c.getId(), c.getLibelle());
    }
}
