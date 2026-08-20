package com.fluttiris.admincontrol.chantier.application;

import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateur;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateurRepository;
import com.fluttiris.admincontrol.chantier.domain.RecurrenceControles;
import com.fluttiris.admincontrol.auth.domain.Utilisateur;
import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChantierService {

    private final ChantierRepository chantierRepository;
    private final ChantierUtilisateurRepository chantierUtilisateurRepository;
    private final AffectationEntrepriseChantierRepository affectationEntrepriseChantierRepository;
    private final UtilisateurRepository utilisateurRepository;

    public Chantier creer(String nom, UUID clientId, String prestation, String adresse, String adresse2,
                           String adresse3, String codePostal, String ville, UUID paysId, String noteInterne,
                           String noteClient, String ipsAllowed, LocalDate dateDebut, LocalDate dateFinPrevue) {
        Chantier chantier = Chantier.creer(nom, clientId, prestation, adresse, adresse2, adresse3, codePostal,
            ville, paysId, noteInterne, noteClient, ipsAllowed, dateDebut, dateFinPrevue);
        return chantierRepository.save(chantier);
    }

    public Chantier modifier(UUID id, String nom, UUID clientId, String prestation, String adresse, String adresse2,
                              String adresse3, String codePostal, String ville, UUID paysId, String noteInterne,
                              String noteClient, String ipsAllowed, LocalDate dateDebut, LocalDate dateFinPrevue) {
        Chantier chantier = obtenir(id);
        chantier.modifier(nom, clientId, prestation, adresse, adresse2, adresse3, codePostal, ville, paysId,
            noteInterne, noteClient, ipsAllowed, dateDebut, dateFinPrevue);
        return chantier;
    }

    public Chantier definirControles(UUID id, RecurrenceControles recurrenceControles, LocalDate dateProchainControle) {
        Chantier chantier = obtenir(id);
        chantier.definirControles(recurrenceControles, dateProchainControle);
        return chantier;
    }

    @Transactional(readOnly = true)
    public Chantier obtenir(UUID id) {
        return chantierRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Chantier", id));
    }

    @Transactional(readOnly = true)
    public List<Chantier> listerParClient(UUID clientId) {
        return chantierRepository.findByClientId(clientId);
    }

    /**
     * Chantiers accessibles par ce compte Client. Deux profils (voir Utilisateur.accesTousChantiers) :
     * - "accès total" : tous les chantiers du Client, sans restriction ;
     * - "responsable de chantier" (par défaut) : uniquement ceux qui lui ont été
     *   explicitement assignés (via chantier_utilisateur, assignation faite depuis la
     *   fiche d'un chantier — voir ChantierUtilisateurController). Sans aucune
     *   assignation, un tel compte ne voit aucun chantier — l'accès reste strictement
     *   opt-in, jamais accordé par défaut.
     */
    @Transactional(readOnly = true)
    public List<UUID> listerIdsAccessiblesPourClient(UUID clientId, UUID utilisateurId) {
        List<UUID> tousLesChantiersDuClient = chantierRepository.findByClientId(clientId).stream()
            .map(Chantier::getId).toList();
        boolean accesTousChantiers = utilisateurRepository.findById(utilisateurId)
            .map(Utilisateur::isAccesTousChantiers).orElse(false);
        if (accesTousChantiers) {
            return tousLesChantiersDuClient;
        }
        List<UUID> assignes = chantierUtilisateurRepository.findByUtilisateurId(utilisateurId).stream()
            .map(ChantierUtilisateur::getChantierId).toList();
        return assignes.stream().filter(tousLesChantiersDuClient::contains).toList();
    }

    @Transactional(readOnly = true)
    public List<Chantier> listerAccessiblesPourClient(UUID clientId, UUID utilisateurId) {
        return chantierRepository.findAllById(listerIdsAccessiblesPourClient(clientId, utilisateurId));
    }

    @Transactional(readOnly = true)
    public List<Chantier> lister() {
        return chantierRepository.findAll();
    }

    /** Chantiers où l'entreprise a (ou a eu) une affectation, quel que soit son rôle (Principale/STT1/STT2). */
    @Transactional(readOnly = true)
    public List<Chantier> listerParEntreprise(UUID entrepriseId) {
        List<UUID> chantierIds = affectationEntrepriseChantierRepository.findByEntrepriseId(entrepriseId).stream()
            .map(AffectationEntrepriseChantier::getChantierId).distinct().toList();
        return chantierRepository.findAllById(chantierIds);
    }

    public Chantier desactiver(UUID id) {
        Chantier chantier = obtenir(id);
        chantier.desactiver();
        return chantier;
    }

    public Chantier activer(UUID id) {
        Chantier chantier = obtenir(id);
        chantier.activer();
        return chantier;
    }

    public Chantier affecterChefChantier(UUID id, UUID utilisateurId) {
        Chantier chantier = obtenir(id);
        chantier.affecterChefChantier(utilisateurId);
        return chantier;
    }

    public Chantier affecterSalarieResponsable(UUID id, UUID salarieId) {
        Chantier chantier = obtenir(id);
        chantier.affecterSalarieResponsable(salarieId);
        return chantier;
    }

    public void supprimer(UUID id) {
        Chantier chantier = obtenir(id);
        chantier.supprimer();
    }

    /** Même rôle que EntrepriseService.raisonSocialeParEntrepriseIds : résout en une seule
        requête les noms de chantier à embarquer dans une réponse d'affectation, pour une
        liste transverse à tous les chantiers (voir AffectationEntrepriseChantierController /
        AffectationSalarieChantierController, endpoint "toutes affectations"). */
    @Transactional(readOnly = true)
    public Map<UUID, String> nomParChantierIds(List<UUID> chantierIds) {
        if (chantierIds.isEmpty()) {
            return Map.of();
        }
        return chantierRepository.findAllById(chantierIds.stream().distinct().toList()).stream()
            .collect(Collectors.toMap(Chantier::getId, Chantier::getNom));
    }
}
