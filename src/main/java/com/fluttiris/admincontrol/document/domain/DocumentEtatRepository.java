package com.fluttiris.admincontrol.document.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentEtatRepository extends JpaRepository<DocumentEtat, UUID> {
}
