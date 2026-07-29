package com.fluttiris.admincontrol.auth.application;

import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public Utilisateur creer(String username, String password, String civilite, String nom, String prenom,
                              String email, Set<String> roles, UUID entrepriseId, UUID clientId) {
        if (utilisateurRepository.existsByUsername(username)) {
            throw new BusinessRuleViolationException("Ce nom d'utilisateur existe déjà");
        }
        Utilisateur utilisateur = Utilisateur.creer(username, passwordEncoder.encode(password), civilite, nom,
            prenom, email, roles, entrepriseId, clientId);
        return utilisateurRepository.save(utilisateur);
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> lister() {
        return utilisateurRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Utilisateur obtenir(UUID id) {
        return utilisateurRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur", id));
    }
}
