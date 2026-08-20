package com.fluttiris.admincontrol.document.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentChantierSupplementaireRepository extends JpaRepository<DocumentChantierSupplementaire, UUID> {

    List<DocumentChantierSupplementaire> findByChantierId(UUID chantierId);

    /** Résolution en lot pour la liste transverse "Affectations" — un seul aller-retour
        quel que soit le nombre de chantiers à résoudre (voir ChantierService.nomParChantierIds
        pour le même principe). */
    List<DocumentChantierSupplementaire> findByChantierIdIn(Collection<UUID> chantierIds);

    Optional<DocumentChantierSupplementaire> findByTypeDocumentIdAndChantierId(UUID typeDocumentId, UUID chantierId);
}
