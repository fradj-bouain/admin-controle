package com.fluttiris.admincontrol.controle.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ControleRepository extends JpaRepository<Controle, UUID> {

    List<Controle> findByChantierId(UUID chantierId);
}
