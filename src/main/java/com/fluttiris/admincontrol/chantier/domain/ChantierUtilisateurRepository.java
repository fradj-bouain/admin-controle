package com.fluttiris.admincontrol.chantier.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChantierUtilisateurRepository extends JpaRepository<ChantierUtilisateur, ChantierUtilisateurId> {
    List<ChantierUtilisateur> findByChantierId(UUID chantierId);
    List<ChantierUtilisateur> findByUtilisateurId(UUID utilisateurId);
    void deleteByChantierIdAndUtilisateurId(UUID chantierId, UUID utilisateurId);
}
