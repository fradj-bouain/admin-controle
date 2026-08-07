package com.fluttiris.admincontrol.entreprise.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffectationEntrepriseChantierRepository extends JpaRepository<AffectationEntrepriseChantier, UUID> {

    List<AffectationEntrepriseChantier> findByChantierId(UUID chantierId);

    List<AffectationEntrepriseChantier> findByChantierIdIn(Collection<UUID> chantierIds);

    List<AffectationEntrepriseChantier> findByEntrepriseId(UUID entrepriseId);

    List<AffectationEntrepriseChantier> findByChantierIdAndEntrepriseId(UUID chantierId, UUID entrepriseId);

    Optional<AffectationEntrepriseChantier> findByChantierIdAndEntrepriseIdAndRole(UUID chantierId, UUID entrepriseId,
                                                                                     RoleEntreprise role);

    List<AffectationEntrepriseChantier> findByAffectationParenteId(UUID affectationParenteId);

    // Affectations "en cours" (sans date de fin, statut ACTIF) pour un lot d'entreprises
    // en une seule requête — évite le N+1 quand on enrichit une liste entière (voir
    // "chantier actuel").
    List<AffectationEntrepriseChantier> findByEntrepriseIdInAndDateFinIsNullAndStatut(
        Collection<UUID> entrepriseIds, String statut);
}
