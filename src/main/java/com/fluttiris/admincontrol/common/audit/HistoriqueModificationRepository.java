package com.fluttiris.admincontrol.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistoriqueModificationRepository extends JpaRepository<HistoriqueModification, UUID> {

    List<HistoriqueModification> findByEntiteAndEntiteIdInOrderByCreatedAtDesc(String entite, List<UUID> entiteIds);
}
