package com.fluttiris.admincontrol.configuration.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActionCorrectiveRepository extends JpaRepository<ActionCorrective, UUID> {
}
