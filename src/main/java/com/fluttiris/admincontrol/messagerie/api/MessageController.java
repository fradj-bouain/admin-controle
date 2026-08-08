package com.fluttiris.admincontrol.messagerie.api;

import com.fluttiris.admincontrol.common.security.CurrentUser;
import com.fluttiris.admincontrol.messagerie.api.dto.MessageResponse;
import com.fluttiris.admincontrol.messagerie.api.dto.SendMessageRequest;
import com.fluttiris.admincontrol.messagerie.application.MessageService;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.Message;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> envoyer(@Valid @RequestBody SendMessageRequest request) {
        var message = messageService.envoyer(currentUser.keycloakId(), request.chantierId(), request.destinataireType(),
            request.destinataireId(), request.sujet(), request.contenu(), request.typeDocumentId(), request.salarieId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(message));
    }

    @GetMapping("/boite-reception")
    @PreAuthorize("isAuthenticated()")
    public List<MessageResponse> boiteReception() {
        // Destinataire toujours dérivé du compte authentifié — jamais accepté en
        // paramètre, pour ne pas permettre de consulter la boîte de réception
        // d'un autre utilisateur. Un message peut cibler ce compte précisément
        // (UTILISATEUR) ou collectivement toute son Entreprise/son Client — les deux
        // formes sont combinées ici (voir MessageService.estDestinataireDe), sinon un
        // message envoyé à "l'Entreprise X" n'apparaît dans AUCUNE boîte de réception.
        List<Message> messages = new ArrayList<>(
            messageService.boiteReception(DestinataireType.UTILISATEUR, currentUser.keycloakId()));
        if (currentUser.entrepriseId().isPresent()) {
            messages.addAll(messageService.boiteReception(DestinataireType.ENTREPRISE, currentUser.entrepriseId().get()));
        }
        if (currentUser.clientId().isPresent()) {
            messages.addAll(messageService.boiteReception(DestinataireType.CLIENT, currentUser.clientId().get()));
        }
        messages.sort(Comparator.comparing(Message::getCreatedAt).reversed());
        return messages.stream().map(MessageResponse::from).toList();
    }

    @GetMapping("/envoyes")
    @PreAuthorize("isAuthenticated()")
    public List<MessageResponse> envoyes() {
        return messageService.messagesEnvoyes(currentUser.keycloakId()).stream().map(MessageResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MessageResponse obtenir(@PathVariable UUID id) {
        return MessageResponse.from(messageService.obtenir(id, currentUser.keycloakId(),
            currentUser.entrepriseId().orElse(null), currentUser.clientId().orElse(null), currentUser.estAdmin()));
    }

    @PostMapping("/{id}/marquer-lu")
    @PreAuthorize("isAuthenticated()")
    public MessageResponse marquerLu(@PathVariable UUID id) {
        return MessageResponse.from(messageService.marquerLu(id, currentUser.keycloakId(),
            currentUser.entrepriseId().orElse(null), currentUser.clientId().orElse(null)));
    }
}
