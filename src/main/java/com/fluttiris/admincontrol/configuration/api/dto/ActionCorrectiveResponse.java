package com.fluttiris.admincontrol.configuration.api.dto;

import com.fluttiris.admincontrol.configuration.domain.ActionCorrective;
import com.fluttiris.admincontrol.configuration.domain.CibleActionCorrective;

import java.util.UUID;

public record ActionCorrectiveResponse(UUID id, String nom, CibleActionCorrective cible) {
    public static ActionCorrectiveResponse from(ActionCorrective a) {
        return new ActionCorrectiveResponse(a.getId(), a.getNom(), a.getCible());
    }
}
