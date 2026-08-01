package com.fluttiris.admincontrol.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HistoriqueModificationService {

    private final HistoriqueModificationRepository repository;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    public void enregistrer(String entite, UUID entiteId, String action, Map<String, Object> details) {
        try {
            String detailsJson = objectMapper.writeValueAsString(details);
            repository.save(HistoriqueModification.enregistrer(entite, entiteId, action, detailsJson, currentUser.keycloakId()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser les détails de l'historique", e);
        }
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationResponse> listerPour(String entite, List<UUID> entiteIds) {
        if (entiteIds.isEmpty()) {
            return List.of();
        }
        return repository.findByEntiteAndEntiteIdInOrderByCreatedAtDesc(entite, entiteIds).stream()
            .map(this::versReponse)
            .toList();
    }

    private HistoriqueModificationResponse versReponse(HistoriqueModification h) {
        Map<String, Object> details;
        try {
            details = objectMapper.readValue(h.getDetails(), new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException e) {
            details = Map.of();
        }
        return new HistoriqueModificationResponse(h.getId(), h.getEntite(), h.getEntiteId(), h.getAction(),
            details, h.getUtilisateurId(), h.getCreatedAt());
    }
}
