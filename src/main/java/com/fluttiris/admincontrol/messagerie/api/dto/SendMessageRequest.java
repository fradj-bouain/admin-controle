package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SendMessageRequest(
    UUID chantierId,
    @NotNull DestinataireType destinataireType,
    @NotNull UUID destinataireId,
    @NotBlank String sujet,
    @NotBlank String contenu,
    // Un ou plusieurs documents demandés en une seule fois (voir Message.typeDocumentIds) —
    // null/vide = ce message n'est pas une demande de document.
    List<UUID> typeDocumentIds,
    UUID salarieId
) {
}
