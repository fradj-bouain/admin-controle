package com.fluttiris.admincontrol.chantier.api.dto;

import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateur;

import java.time.Instant;
import java.util.UUID;

public record ChantierUtilisateurResponse(UUID chantierId, UUID utilisateurId, Instant createdAt) {
    public static ChantierUtilisateurResponse from(ChantierUtilisateur a) {
        return new ChantierUtilisateurResponse(a.getChantierId(), a.getUtilisateurId(), a.getCreatedAt());
    }
}
