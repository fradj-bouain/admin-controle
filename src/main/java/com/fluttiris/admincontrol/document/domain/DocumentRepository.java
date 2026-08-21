package com.fluttiris.admincontrol.document.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findBySalarieId(UUID salarieId);

    List<Document> findByEntrepriseId(UUID entrepriseId);

    List<Document> findByDateExpiration(LocalDate dateExpiration);

    List<Document> findByStatutValidationOrderByCreatedAtDesc(StatutValidation statutValidation);

    List<Document> findByStatutValidationAndDateExpirationBetweenOrderByDateExpirationAsc(
        StatutValidation statutValidation, LocalDate debut, LocalDate fin);

    List<Document> findByEntrepriseIdIsNotNull();

    /** Les documents d'une entreprise propres à UN chantier précis (checklist de la fiche
        Entreprise vue depuis l'onglet "sur ce chantier") — indépendants de ses documents
        globaux (chantierId null, voir {@link #findByEntrepriseId}) et de ses documents sur
        les autres chantiers : modifier/supprimer une ligne d'ici ne touche jamais les autres
        chantiers, chacun ayant sa propre instance de document (voir DocumentChantierSupplementaire). */
    List<Document> findByEntrepriseIdAndChantierId(UUID entrepriseId, UUID chantierId);

    /** Même principe que {@link #findByEntrepriseIdAndChantierId}, côté salarié — instance
        indépendante par chantier, jamais partagée entre deux chantiers d'un même salarié. */
    List<Document> findBySalarieIdAndChantierId(UUID salarieId, UUID chantierId);

    /** Résolution en lot du nombre de documents déposés par affectation, pour la liste
        transverse "Affectations" (GET /entreprises/affectations) — un seul aller-retour,
        regroupé ensuite en mémoire par (entrepriseId, chantierId), jamais une requête par
        affectation (même principe que ChantierService.nomParChantierIds). */
    List<Document> findByEntrepriseIdInAndChantierIdIn(Collection<UUID> entrepriseIds, Collection<UUID> chantierIds);
}
