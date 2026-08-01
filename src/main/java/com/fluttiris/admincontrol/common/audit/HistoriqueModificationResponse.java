package com.fluttiris.admincontrol.common.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record HistoriqueModificationResponse(
    UUID id,
    String entite,
    UUID entiteId,
    String action,
    Map<String, Object> details,
    UUID utilisateurId,
    Instant createdAt
) {
}
