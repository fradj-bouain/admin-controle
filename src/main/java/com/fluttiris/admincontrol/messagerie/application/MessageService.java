package com.fluttiris.admincontrol.messagerie.application;

import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.Message;
import com.fluttiris.admincontrol.messagerie.domain.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;

    public Message envoyer(UUID expediteurId, UUID chantierId, DestinataireType type, UUID destinataireId,
                            String sujet, String contenu) {
        Message message = Message.envoyer(expediteurId, chantierId, type, destinataireId, sujet, contenu);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> boiteReception(DestinataireType type, UUID destinataireId) {
        return messageRepository.findByDestinataireTypeAndDestinataireIdOrderByCreatedAtDesc(type, destinataireId);
    }

    @Transactional(readOnly = true)
    public List<Message> messagesEnvoyes(UUID expediteurId) {
        return messageRepository.findByExpediteurUtilisateurIdOrderByCreatedAtDesc(expediteurId);
    }

    @Transactional(readOnly = true)
    public Message obtenir(UUID id, UUID utilisateurId, boolean estAdmin) {
        Message message = messageRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Message", id));
        boolean estExpediteur = message.getExpediteurUtilisateurId().equals(utilisateurId);
        boolean estDestinataire = message.getDestinataireType() == DestinataireType.UTILISATEUR
            && message.getDestinataireId().equals(utilisateurId);
        if (!estAdmin && !estExpediteur && !estDestinataire) {
            throw new AccessDeniedException("Ce message ne vous appartient pas");
        }
        return message;
    }

    public Message marquerLu(UUID id, UUID utilisateurId) {
        Message message = messageRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Message", id));
        // Seul le véritable destinataire (compte Utilisateur ciblé) peut marquer un
        // message comme lu — sinon n'importe quel compte authentifié pourrait
        // manipuler le statut de lecture de messages d'autrui.
        if (message.getDestinataireType() != DestinataireType.UTILISATEUR || !message.getDestinataireId().equals(utilisateurId)) {
            throw new AccessDeniedException("Ce message ne vous est pas destiné");
        }
        message.marquerLu(utilisateurId);
        return message;
    }
}
