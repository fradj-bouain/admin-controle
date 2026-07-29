package com.fluttiris.admincontrol.configuration.api.dto;

import com.fluttiris.admincontrol.configuration.domain.TypeSalarie;

import java.util.UUID;

public record TypeSalarieResponse(UUID id, String code, String libelle) {
    public static TypeSalarieResponse from(TypeSalarie t) {
        return new TypeSalarieResponse(t.getId(), t.getCode(), t.getLibelle());
    }
}
