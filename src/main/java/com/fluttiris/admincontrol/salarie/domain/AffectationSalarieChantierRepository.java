package com.fluttiris.admincontrol.salarie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AffectationSalarieChantierRepository extends JpaRepository<AffectationSalarieChantier, UUID> {

    List<AffectationSalarieChantier> findByChantierId(UUID chantierId);

    List<AffectationSalarieChantier> findByChantierIdIn(Collection<UUID> chantierIds);

    // Salariés d'UNE entreprise sur CE chantier précisément — voir règle validée "Entreprise +
    // Chantier = contexte d'affectation" (SalarieService.listerParEntrepriseEtChantier). Passe
    // par affectationEntrepriseChantierId (pas entrepriseId, absent de cette table) car une
    // même entreprise peut porter plusieurs rôles sur ce même chantier (Principale ET STT1).
    List<AffectationSalarieChantier> findByChantierIdAndAffectationEntrepriseChantierIdIn(
        UUID chantierId, Collection<UUID> affectationEntrepriseChantierIds);

    List<AffectationSalarieChantier> findBySalarieId(UUID salarieId);

    // Affectations "en cours" (sans date de fin) pour un lot de salariés en une seule
    // requête — évite le N+1 quand on enrichit une liste entière (voir "chantier actuel").
    List<AffectationSalarieChantier> findBySalarieIdInAndDateFinIsNull(Collection<UUID> salarieIds);

    boolean existsBySalarieIdAndChantierIdAndDateFinIsNull(UUID salarieId, UUID chantierId);
}
