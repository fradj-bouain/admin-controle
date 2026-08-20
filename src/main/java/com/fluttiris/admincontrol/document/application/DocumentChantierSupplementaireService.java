package com.fluttiris.admincontrol.document.application;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.document.domain.DocumentChantierSupplementaire;
import com.fluttiris.admincontrol.document.domain.DocumentChantierSupplementaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Types de document demandés EN PLUS sur un chantier précis, par-dessus les types
 * obligatoires globaux (voir DocumentChantierSupplementaire — aucune ligne n'est
 * nécessaire pour un type déjà obligatoire partout).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentChantierSupplementaireService {

    private final DocumentChantierSupplementaireRepository repository;

    public DocumentChantierSupplementaire ajouter(UUID chantierId, UUID typeDocumentId) {
        repository.findByTypeDocumentIdAndChantierId(typeDocumentId, chantierId).ifPresent(existante -> {
            throw new BusinessRuleViolationException("Ce type de document est déjà demandé en plus sur ce chantier");
        });
        return repository.save(DocumentChantierSupplementaire.creer(typeDocumentId, chantierId));
    }

    public void retirer(UUID id) {
        DocumentChantierSupplementaire regle = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Document supplémentaire de chantier", id));
        regle.supprimer();
    }

    @Transactional(readOnly = true)
    public List<DocumentChantierSupplementaire> listerParChantier(UUID chantierId) {
        return repository.findByChantierId(chantierId);
    }

    /** Résout, en une seule requête, les types de document supplémentaires de plusieurs
        chantiers à la fois — voir ChantierService.nomParChantierIds pour le même principe
        (jamais une requête par chantier). */
    @Transactional(readOnly = true)
    public Map<UUID, Set<UUID>> typeDocumentIdsParChantierIds(Collection<UUID> chantierIds) {
        if (chantierIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Set<UUID>> resultat = new HashMap<>();
        for (DocumentChantierSupplementaire regle : repository.findByChantierIdIn(chantierIds)) {
            resultat.computeIfAbsent(regle.getChantierId(), k -> new HashSet<>()).add(regle.getTypeDocumentId());
        }
        return resultat;
    }
}
