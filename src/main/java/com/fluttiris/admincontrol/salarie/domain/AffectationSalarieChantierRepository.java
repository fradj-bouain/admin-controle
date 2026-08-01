package com.fluttiris.admincontrol.salarie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AffectationSalarieChantierRepository extends JpaRepository<AffectationSalarieChantier, UUID> {

    List<AffectationSalarieChantier> findByChantierId(UUID chantierId);

    List<AffectationSalarieChantier> findByChantierIdIn(Collection<UUID> chantierIds);

    List<AffectationSalarieChantier> findBySalarieId(UUID salarieId);

    boolean existsBySalarieIdAndChantierIdAndDateFinIsNull(UUID salarieId, UUID chantierId);
}
