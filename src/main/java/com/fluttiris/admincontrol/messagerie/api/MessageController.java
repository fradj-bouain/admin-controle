package com.fluttiris.admincontrol.messagerie.api;

import com.fluttiris.admincontrol.common.security.CurrentUser;
import com.fluttiris.admincontrol.messagerie.api.dto.MessageResponse;
import com.fluttiris.admincontrol.messagerie.api.dto.SendMessageRequest;
import com.fluttiris.admincontrol.messagerie.application.MessageService;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            request.destinataireId(), request.sujet(), request.contenu());
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(message));
    }

    @GetMapping("/boite-reception")
    @PreAuthorize("isAuthenticated()")
    public List<MessageResponse> boiteReception(@RequestParam DestinataireType type, @RequestParam UUID destinataireId) {
        return messageService.boiteReception(type, destinataireId).stream().map(MessageResponse::from).toList();
    }

    @GetMapping("/envoyes")
    @PreAuthorize("isAuthenticated()")
    public List<MessageResponse> envoyes() {
        return messageService.messagesEnvoyes(currentUser.keycloakId()).stream().map(MessageResponse::from).toList();
    }

    @PostMapping("/{id}/marquer-lu")
    @PreAuthorize("isAuthenticated()")
    public MessageResponse marquerLu(@PathVariable UUID id) {
        return MessageResponse.from(messageService.marquerLu(id, currentUser.keycloakId()));
    }
}
