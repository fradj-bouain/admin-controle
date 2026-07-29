package com.fluttiris.admincontrol.messagerie.application;

import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.messagerie.domain.CibleGroupe;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanificationService {

    private final MessagePlanifieRepository messagePlanifieRepository;

    public MessagePlanifie planifier(UUID expediteurUtilisateurId, CibleGroupe cibleGroupe,
                                      DestinataireType destinataireType, UUID destinataireId, UUID chantierId,
                                      String sujet, String contenu, Instant dateEnvoiPrevue) {
        MessagePlanifie message = MessagePlanifie.planifier(expediteurUtilisateurId, cibleGroupe, destinataireType,
            destinataireId, chantierId, sujet, contenu, dateEnvoiPrevue);
        return messagePlanifieRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<MessagePlanifie> lister() {
        return messagePlanifieRepository.findAllByOrderByCreatedAtDesc();
    }

    public MessagePlanifie annuler(UUID id) {
        MessagePlanifie message = messagePlanifieRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Message planifié", id));
        message.annuler();
        return message;
    }
}
