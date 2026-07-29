package com.fluttiris.admincontrol.messagerie.api.dto;

import com.fluttiris.admincontrol.messagerie.domain.CibleGroupe;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.StatutMessagePlanifie;

import java.time.Instant;
import java.util.UUID;

public record MessagePlanifieResponse(
    UUID id,
    UUID regleId,
    UUID expediteurUtilisateurId,
    CibleGroupe cibleGroupe,
    DestinataireType destinataireType,
    UUID destinataireId,
    UUID chantierId,
    String sujet,
    String contenu,
    Instant dateEnvoiPrevue,
    StatutMessagePlanifie statut,
    Instant dateEnvoiReelle,
    Instant createdAt
) {
    public static MessagePlanifieResponse from(MessagePlanifie m) {
        return new MessagePlanifieResponse(m.getId(), m.getRegleId(), m.getExpediteurUtilisateurId(),
            m.getCibleGroupe(), m.getDestinataireType(), m.getDestinataireId(), m.getChantierId(), m.getSujet(),
            m.getContenu(), m.getDateEnvoiPrevue(), m.getStatut(), m.getDateEnvoiReelle(), m.getCreatedAt());
    }
}
