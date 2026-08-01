package com.fluttiris.admincontrol.controle.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ControleSalarieRepository extends JpaRepository<ControleSalarie, UUID> {

    List<ControleSalarie> findByControleIdOrderByCreatedAtAsc(UUID controleId);
}
