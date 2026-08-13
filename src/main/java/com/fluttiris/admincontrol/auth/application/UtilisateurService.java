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
                              String email, Set<String> roles, UUID entrepriseId, UUID clientId, UUID controleTiersId) {
        if (utilisateurRepository.existsByUsername(username)) {
            throw new BusinessRuleViolationException("Ce nom d'utilisateur existe déjà");
        }
        Utilisateur utilisateur = Utilisateur.creer(username, passwordEncoder.encode(password), civilite, nom,
            prenom, email, roles, entrepriseId, clientId, controleTiersId);
        return utilisateurRepository.save(utilisateur);
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> lister() {
        return utilisateurRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> listerParClient(UUID clientId) {
        return utilisateurRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> listerParEntreprise(UUID entrepriseId) {
        return utilisateurRepository.findByEntrepriseId(entrepriseId);
    }

    @Transactional(readOnly = true)
    public Utilisateur obtenir(UUID id) {
        return utilisateurRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur", id));
    }

    /** password null/vide = mot de passe inchangé (édition depuis Mon équipe : le champ y
        est optionnel, contrairement à la création). */
    public Utilisateur modifier(UUID id, String nom, String prenom, String email, String username, String password) {
        Utilisateur utilisateur = obtenir(id);
        if (!username.equals(utilisateur.getUsername()) && utilisateurRepository.existsByUsername(username)) {
            throw new BusinessRuleViolationException("Ce nom d'utilisateur existe déjà");
        }
        utilisateur.modifier(nom, prenom, email, username);
        if (password != null && !password.isBlank()) {
            utilisateur.changerMotDePasse(passwordEncoder.encode(password));
        }
        return utilisateur;
    }

    public Utilisateur desactiver(UUID id) {
        Utilisateur utilisateur = obtenir(id);
        utilisateur.desactiver();
        return utilisateur;
    }

    public Utilisateur activer(UUID id) {
        Utilisateur utilisateur = obtenir(id);
        utilisateur.activer();
        return utilisateur;
    }

    public void supprimer(UUID id) {
        obtenir(id).supprimer();
    }
}
