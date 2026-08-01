package com.fluttiris.admincontrol.controle.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RapportControleRepository extends JpaRepository<RapportControle, UUID> {

    Optional<RapportControle> findByControleId(UUID controleId);

    List<RapportControle> findAllByOrderByCreatedAtDesc();

    List<RapportControle> findByControleIdInOrderByCreatedAtDesc(List<UUID> controleIds);
}
