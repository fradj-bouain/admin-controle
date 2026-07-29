package com.fluttiris.admincontrol.controle.api.dto;

import com.fluttiris.admincontrol.controle.domain.Controle;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ControleResponse(
    UUID id,
    UUID chantierId,
    UUID controleurUtilisateurId,
    LocalDate dateControle,
    String remarques,
    UUID controleTiersId,
    LocalDate dateFin,
    boolean termine,
    Instant updatedAt
) {
    public static ControleResponse from(Controle c) {
        return new ControleResponse(c.getId(), c.getChantierId(), c.getControleurUtilisateurId(),
            c.getDateControle(), c.getRemarques(), c.getControleTiersId(), c.getDateFin(), c.isTermine(),
            c.getUpdatedAt());
    }
}
