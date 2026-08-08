package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendMessageRequest(
    UUID chantierId,
    @NotNull DestinataireType destinataireType,
    @NotNull UUID destinataireId,
    @NotBlank String sujet,
    @NotBlank String contenu,
    UUID typeDocumentId,
    UUID salarieId
) {
}
