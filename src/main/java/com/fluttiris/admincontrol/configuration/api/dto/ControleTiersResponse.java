package com.fluttiris.admincontrol.configuration.api.dto;

import com.fluttiris.admincontrol.configuration.domain.ControleTiers;

import java.util.UUID;

public record ControleTiersResponse(UUID id, String nom) {
    public static ControleTiersResponse from(ControleTiers c) {
        return new ControleTiersResponse(c.getId(), c.getNom());
    }
}
