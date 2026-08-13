package com.fluttiris.admincontrol.entreprise.application;

import com.fluttiris.admincontrol.chantier.application.ChantierService;
import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierRepository;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseCreeeEvent;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final ChantierService chantierService;
    private final ChantierRepository chantierRepository;
    private final AffectationEntrepriseChantierRepository affectationEntrepriseChantierRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Entreprise creer(String raisonSociale, String siret, String adresse, String adresse2, String adresse3,
                             String codePostal, String ville, UUID paysId, UUID corpsDeMetierId, String telephone,
                             String telephone2, String telephone3, String fax, String email, String email2,
                             String email3, String formeJuridique, String siren, String rcsRci, String tvaIntra,
                             String numCotisant, String responsableSignataireAgrement, String commentaire) {
        Entreprise entreprise = Entreprise.creer(raisonSociale, siret, adresse, adresse2, adresse3, codePostal,
            ville, paysId, corpsDeMetierId, telephone, telephone2, telephone3, fax, email, email2, email3,
            formeJuridique, siren, rcsRci, tvaIntra, numCotisant, responsableSignataireAgrement, commentaire);
        entreprise = entrepriseRepository.save(entreprise);
        eventPublisher.publishEvent(new EntrepriseCreeeEvent(entreprise.getId()));
        return entreprise;
    }

    public Entreprise modifier(UUID id, String raisonSociale, String siret, String adresse, String adresse2,
                                String adresse3, String codePostal, String ville, UUID paysId, UUID corpsDeMetierId,
                                String telephone, String telephone2, String telephone3, String fax, String email,
                                String email2, String email3, String formeJuridique, String siren, String rcsRci,
                                String tvaIntra, String numCotisant, String responsableSignataireAgrement,
                                String commentaire) {
        Entreprise entreprise = obtenir(id);
        entreprise.modifier(raisonSociale, siret, adresse, adresse2, adresse3, codePostal, ville, paysId,
            corpsDeMetierId, telephone, telephone2, telephone3, fax, email, email2, email3, formeJuridique, siren,
            rcsRci, tvaIntra, numCotisant, responsableSignataireAgrement, commentaire);
        return entreprise;
    }

    @Transactional(readOnly = true)
    public Entreprise obtenir(UUID id) {
        return entrepriseRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise", id));
    }

    @Transactional(readOnly = true)
    public List<Entreprise> lister() {
        return entrepriseRepository.findAll();
    }

    /** Entreprises affectées à un chantier accessible par ce client (tous ses chantiers, ou
        seulement ceux assignés à cet utilisateur — voir ChantierService), tous rôles
        d'entreprise confondus (Principale/STT1/STT2). */
    @Transactional(readOnly = true)
    public List<Entreprise> listerParClient(UUID clientId, UUID utilisateurId) {
        List<UUID> chantierIds = chantierService.listerIdsAccessiblesPourClient(clientId, utilisateurId);
        if (chantierIds.isEmpty()) {
            return List.of();
        }
        List<UUID> entrepriseIds = affectationEntrepriseChantierRepository.findByChantierIdIn(chantierIds).stream()
            .map(AffectationEntrepriseChantier::getEntrepriseId).distinct().toList();
        return entrepriseRepository.findAllById(entrepriseIds);
    }

    public Entreprise desactiver(UUID id) {
        Entreprise entreprise = obtenir(id);
        entreprise.desactiver();
        return entreprise;
    }

    public Entreprise activer(UUID id) {
        Entreprise entreprise = obtenir(id);
        entreprise.activer();
        return entreprise;
    }

    public void supprimer(UUID id) {
        Entreprise entreprise = obtenir(id);
        entreprise.supprimer();
    }

    /**
     * Chantier "en cours" (affectation active, sans date de fin) pour chaque entreprise
     * du lot — à ne pas confondre avec le champ `actif` de l'entreprise (simple
     * interrupteur administratif, sans lien avec une affectation chantier réelle ; voir
     * audit UX). Une entreprise absente du résultat n'a aucune affectation en cours
     * ("Disponible" côté affichage — décision volontairement laissée au frontend). En
     * une seule requête batch, pas de N+1 sur la liste complète.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> chantierActuelParEntreprise(List<UUID> entrepriseIds) {
        if (entrepriseIds.isEmpty()) {
            return Map.of();
        }
        List<AffectationEntrepriseChantier> enCours = affectationEntrepriseChantierRepository
            .findByEntrepriseIdInAndDateFinIsNullAndStatut(entrepriseIds, "ACTIF");
        if (enCours.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> nomChantierParId = chantierRepository.findAllById(
            enCours.stream().map(AffectationEntrepriseChantier::getChantierId).distinct().toList()
        ).stream().collect(Collectors.toMap(Chantier::getId, Chantier::getNom));

        Map<UUID, List<String>> nomsParEntreprise = new HashMap<>();
        for (AffectationEntrepriseChantier a : enCours) {
            nomsParEntreprise.computeIfAbsent(a.getEntrepriseId(), k -> new ArrayList<>())
                .add(nomChantierParId.getOrDefault(a.getChantierId(), ""));
        }
        Map<UUID, String> resultat = new HashMap<>();
        for (var entry : nomsParEntreprise.entrySet()) {
            List<String> noms = entry.getValue();
            resultat.put(entry.getKey(), noms.size() == 1 ? noms.get(0) : noms.size() + " chantiers en cours");
        }
        return resultat;
    }

    /**
     * Résout en une seule requête batch la raison sociale de chaque entreprise — utilisé pour
     * afficher les sous-traitants d'un chantier (voir AffectationEntrepriseChantierController) :
     * une Entreprise n'a pas le droit de consulter la fiche d'une autre entreprise (obtenir()),
     * donc le nom doit être embarqué dans la réponse d'affectation plutôt que résolu par le front.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> raisonSocialeParEntrepriseIds(List<UUID> entrepriseIds) {
        if (entrepriseIds.isEmpty()) {
            return Map.of();
        }
        return entrepriseRepository.findAllById(entrepriseIds.stream().distinct().toList()).stream()
            .collect(Collectors.toMap(Entreprise::getId, Entreprise::getRaisonSociale));
    }
}
