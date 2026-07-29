package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.CibleGroupe;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisation;

import java.util.UUID;

public record RegleAutomatisationResponse(
    UUID id,
    String nom,
    String champSurveillableId,
    int nbJoursAvant,
    boolean actif,
    CibleGroupe cibleGroupe,
    DestinataireType destinataireType,
    UUID destinataireId,
    String sujet,
    String contenu
) {
    public static RegleAutomatisationResponse from(RegleAutomatisation r) {
        return new RegleAutomatisationResponse(r.getId(), r.getNom(), r.getChampSurveillableId(), r.getNbJoursAvant(),
            r.isActif(), r.getCibleGroupe(), r.getDestinataireType(), r.getDestinataireId(), r.getSujet(), r.getContenu());
    }
}
