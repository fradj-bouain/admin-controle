package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.CibleGroupe;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisation;
import com.fluttiris.admincontrol.messagerie.domain.TypeDeclencheur;

import java.time.Instant;
import java.util.UUID;

public record RegleAutomatisationResponse(
    UUID id,
    String nom,
    TypeDeclencheur typeDeclencheur,
    String champSurveillableId,
    int nbJoursAvant,
    boolean actif,
    CibleGroupe cibleGroupe,
    DestinataireType destinataireType,
    UUID destinataireId,
    String sujet,
    String contenu,
    String numeroInterne,
    String titreInterne,
    int nbEnvois,
    Instant dernierEnvoi
) {
    public static RegleAutomatisationResponse from(RegleAutomatisation r) {
        return new RegleAutomatisationResponse(r.getId(), r.getNom(), r.getTypeDeclencheur(), r.getChampSurveillableId(),
            r.getNbJoursAvant(), r.isActif(), r.getCibleGroupe(), r.getDestinataireType(), r.getDestinataireId(),
            r.getSujet(), r.getContenu(), r.getNumeroInterne(), r.getTitreInterne(), r.getNbEnvois(), r.getDernierEnvoi());
    }
}
