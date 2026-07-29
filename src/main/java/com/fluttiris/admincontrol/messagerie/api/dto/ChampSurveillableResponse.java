package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.ChampSurveillable;

public record ChampSurveillableResponse(String id, String entiteLibelle, String champLibelle) {
    public static ChampSurveillableResponse from(ChampSurveillable c) {
        return new ChampSurveillableResponse(c.getId(), c.getEntiteLibelle(), c.getChampLibelle());
    }
}
