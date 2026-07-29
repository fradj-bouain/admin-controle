package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.CibleGroupe;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CreateRegleAutomatisationRequest(
    @NotBlank String nom,
    @NotBlank String champSurveillableId,
    @PositiveOrZero int nbJoursAvant,
    @NotNull CibleGroupe cibleGroupe,
    DestinataireType destinataireType,
    UUID destinataireId,
    @NotBlank String sujet,
    @NotBlank String contenu
) {
}
