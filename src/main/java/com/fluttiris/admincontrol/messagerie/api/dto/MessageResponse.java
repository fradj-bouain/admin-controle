package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID expediteurUtilisateurId,
    UUID chantierId,
    DestinataireType destinataireType,
    UUID destinataireId,
    String sujet,
    String contenu,
    boolean lu,
    UUID luParUtilisateurId,
    Instant dateLu,
    Instant createdAt
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getExpediteurUtilisateurId(), m.getChantierId(),
            m.getDestinataireType(), m.getDestinataireId(), m.getSujet(), m.getContenu(), m.isLu(),
            m.getLuParUtilisateurId(), m.getDateLu(), m.getCreatedAt());
    }
}
