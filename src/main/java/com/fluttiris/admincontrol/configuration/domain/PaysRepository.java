package com.fluttiris.admincontrol.configuration.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaysRepository extends JpaRepository<Pays, UUID> {

    Optional<Pays> findByCodeIso(String codeIso);
}
