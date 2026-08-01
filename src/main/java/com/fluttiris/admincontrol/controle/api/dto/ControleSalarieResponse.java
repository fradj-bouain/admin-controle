package com.fluttiris.admincontrol.controle.api.dto;

import com.fluttiris.admincontrol.controle.domain.ControleSalarie;

import java.time.Instant;
import java.util.UUID;

public record ControleSalarieResponse(
    UUID id,
    UUID controleId,
    UUID salarieId,
    UUID entrepriseId,
    boolean accorde,
    UUID actionCorrectiveId,
    Instant createdAt
) {
    public static ControleSalarieResponse from(ControleSalarie c) {
        return new ControleSalarieResponse(c.getId(), c.getControleId(), c.getSalarieId(), c.getEntrepriseId(),
            c.isAccorde(), c.getActionCorrectiveId(), c.getCreatedAt());
    }
}
