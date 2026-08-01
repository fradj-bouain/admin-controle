package com.fluttiris.admincontrol.document.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findBySalarieId(UUID salarieId);

    List<Document> findByEntrepriseId(UUID entrepriseId);

    List<Document> findByDateExpiration(LocalDate dateExpiration);

    List<Document> findByStatutValidationOrderByCreatedAtDesc(StatutValidation statutValidation);
}
