package com.fluttiris.admincontrol.messagerie.api;

import com.fluttiris.admincontrol.common.security.CurrentUser;
import com.fluttiris.admincontrol.messagerie.api.dto.MessagePlanifieResponse;
import com.fluttiris.admincontrol.messagerie.api.dto.PlanifierMessageRequest;
import com.fluttiris.admincontrol.messagerie.application.PlanificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages-planifies")
@RequiredArgsConstructor
public class MessagePlanifieController {

    private final PlanificationService planificationService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessagePlanifieResponse> planifier(@Valid @RequestBody PlanifierMessageRequest request) {
        var message = planificationService.planifier(currentUser.keycloakId(), request.cibleGroupe(),
            request.destinataireType(), request.destinataireId(), request.chantierId(), request.sujet(),
            request.contenu(), request.dateEnvoiPrevue());
        return ResponseEntity.status(HttpStatus.CREATED).body(MessagePlanifieResponse.from(message));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MessagePlanifieResponse> lister() {
        return planificationService.lister().stream().map(MessagePlanifieResponse::from).toList();
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("isAuthenticated()")
    public MessagePlanifieResponse annuler(@PathVariable UUID id) {
        return MessagePlanifieResponse.from(planificationService.annuler(id));
    }
}
