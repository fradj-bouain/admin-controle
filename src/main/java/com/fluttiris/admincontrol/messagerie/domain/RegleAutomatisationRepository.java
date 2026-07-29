package com.fluttiris.admincontrol.messagerie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegleAutomatisationRepository extends JpaRepository<RegleAutomatisation, UUID> {

    List<RegleAutomatisation> findByActifTrue();
}
