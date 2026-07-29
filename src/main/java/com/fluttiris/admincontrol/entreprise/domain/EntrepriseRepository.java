package com.fluttiris.admincontrol.entreprise.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EntrepriseRepository extends JpaRepository<Entreprise, UUID> {
}
