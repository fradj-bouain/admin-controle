package com.fluttiris.admincontrol.entreprise.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffectationEntrepriseChantierRepository extends JpaRepository<AffectationEntrepriseChantier, UUID> {

    List<AffectationEntrepriseChantier> findByChantierId(UUID chantierId);

    List<AffectationEntrepriseChantier> findByEntrepriseId(UUID entrepriseId);

    List<AffectationEntrepriseChantier> findByChantierIdAndEntrepriseId(UUID chantierId, UUID entrepriseId);

    Optional<AffectationEntrepriseChantier> findByChantierIdAndEntrepriseIdAndRole(UUID chantierId, UUID entrepriseId,
                                                                                     RoleEntreprise role);

    List<AffectationEntrepriseChantier> findByAffectationParenteId(UUID affectationParenteId);
}
