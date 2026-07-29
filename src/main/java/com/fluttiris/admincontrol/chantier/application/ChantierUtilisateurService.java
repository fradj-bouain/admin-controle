package com.fluttiris.admincontrol.chantier.application;

import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateur;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gère l'accès d'un utilisateur (côté client) à un chantier précis —
 * brique nécessaire pour un futur portail client en lecture seule.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChantierUtilisateurService {

    private final ChantierUtilisateurRepository chantierUtilisateurRepository;

    public ChantierUtilisateur accorder(UUID chantierId, UUID utilisateurId) {
        return chantierUtilisateurRepository.save(ChantierUtilisateur.creer(chantierId, utilisateurId));
    }

    @Transactional(readOnly = true)
    public List<ChantierUtilisateur> lister(UUID chantierId) {
        return chantierUtilisateurRepository.findByChantierId(chantierId);
    }

    public void revoquer(UUID chantierId, UUID utilisateurId) {
        chantierUtilisateurRepository.deleteByChantierIdAndUtilisateurId(chantierId, utilisateurId);
    }
}
