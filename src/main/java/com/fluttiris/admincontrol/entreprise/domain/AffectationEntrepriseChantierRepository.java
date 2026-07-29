package com.fluttiris.admincontrol.entreprise.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffectationEntrepriseChantierRepository extends JpaRepository<AffectationEntrepriseChantier, UUID> {

    List<AffectationEntrepriseChantier> findByChantierId(UUID chantierId);

    List<AffectationEntrepriseChantier> findByEntrepriseId(UUID entrepriseId);

    Optional<AffectationEntrepriseChantier> findByChantierIdAndEntrepriseId(UUID chantierId, UUID entrepriseId);

    List<AffectationEntrepriseChantier> findByAffectationParenteId(UUID affectationParenteId);
}
