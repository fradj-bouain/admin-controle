package com.fluttiris.admincontrol.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Utilisateur> findByActifTrue();
}
