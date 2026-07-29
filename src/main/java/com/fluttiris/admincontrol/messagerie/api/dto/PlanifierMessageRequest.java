package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.CibleGroupe;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record PlanifierMessageRequest(
    @NotNull CibleGroupe cibleGroupe,
    DestinataireType destinataireType,
    UUID destinataireId,
    UUID chantierId,
    @NotBlank String sujet,
    @NotBlank String contenu,
    @NotNull Instant dateEnvoiPrevue
) {
}
