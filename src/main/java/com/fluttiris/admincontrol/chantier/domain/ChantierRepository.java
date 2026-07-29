package com.fluttiris.admincontrol.chantier.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ChantierRepository extends JpaRepository<Chantier, UUID> {

    List<Chantier> findByClientId(UUID clientId);

    List<Chantier> findByDateProchainControle(LocalDate dateProchainControle);
}
