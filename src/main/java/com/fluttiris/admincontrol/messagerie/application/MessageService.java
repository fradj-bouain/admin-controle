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
                            String sujet, String contenu, UUID typeDocumentId, UUID salarieId) {
        Message message = Message.envoyer(expediteurId, chantierId, type, destinataireId, sujet, contenu,
            typeDocumentId, salarieId);
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
    public Message obtenir(UUID id, UUID utilisateurId, UUID entrepriseId, UUID clientId, boolean estAdmin) {
        Message message = messageRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Message", id));
        boolean estExpediteur = message.getExpediteurUtilisateurId().equals(utilisateurId);
        if (!estAdmin && !estExpediteur && !estDestinataireDe(message, utilisateurId, entrepriseId, clientId)) {
            throw new AccessDeniedException("Ce message ne vous appartient pas");
        }
        return message;
    }

    public Message marquerLu(UUID id, UUID utilisateurId, UUID entrepriseId, UUID clientId) {
        Message message = messageRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Message", id));
        // Le véritable destinataire — le compte Utilisateur ciblé, ou n'importe quel
        // compte de l'Entreprise/du Client visé collectivement — peut seul marquer un
        // message comme lu, sinon n'importe quel compte authentifié pourrait manipuler
        // le statut de lecture de messages d'autrui.
        if (!estDestinataireDe(message, utilisateurId, entrepriseId, clientId)) {
            throw new AccessDeniedException("Ce message ne vous est pas destiné");
        }
        message.marquerLu(utilisateurId);
        return message;
    }

    /** Un message peut cibler un compte Utilisateur précis, ou collectivement toute une
        Entreprise / tout un Client — dans ce cas n'importe quel compte de cette
        entreprise/ce client est un destinataire légitime, pas seulement son créateur. */
    private boolean estDestinataireDe(Message message, UUID utilisateurId, UUID entrepriseId, UUID clientId) {
        return switch (message.getDestinataireType()) {
            case UTILISATEUR -> message.getDestinataireId().equals(utilisateurId);
            case ENTREPRISE -> entrepriseId != null && message.getDestinataireId().equals(entrepriseId);
            case CLIENT -> clientId != null && message.getDestinataireId().equals(clientId);
        };
    }
}
