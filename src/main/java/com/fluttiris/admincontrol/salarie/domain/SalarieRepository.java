package com.fluttiris.admincontrol.salarie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SalarieRepository extends JpaRepository<Salarie, UUID> {

    List<Salarie> findByEntrepriseEmployeurId(UUID entrepriseId);

    List<Salarie> findByStatut(StatutSalarie statut);
}
