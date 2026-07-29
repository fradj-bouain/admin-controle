package com.fluttiris.admincontrol.configuration.api.dto;

import com.fluttiris.admincontrol.configuration.domain.TypeContratSalarie;

import java.util.UUID;

public record TypeContratSalarieResponse(UUID id, String code, String libelle) {
    public static TypeContratSalarieResponse from(TypeContratSalarie t) {
        return new TypeContratSalarieResponse(t.getId(), t.getCode(), t.getLibelle());
    }
}
