package com.fluttiris.admincontrol.controle.application;

import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.client.domain.Client;
import com.fluttiris.admincontrol.client.domain.ClientRepository;
import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.common.mail.EmailService;
import com.fluttiris.admincontrol.controle.api.dto.RapportControleResponse;
import com.fluttiris.admincontrol.controle.domain.Controle;
import com.fluttiris.admincontrol.controle.domain.ControleRepository;
import com.fluttiris.admincontrol.controle.domain.ControleSalarie;
import com.fluttiris.admincontrol.controle.domain.ControleSalarieRepository;
import com.fluttiris.admincontrol.controle.domain.RapportControle;
import com.fluttiris.admincontrol.controle.domain.RapportControleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ControleService {

    private final ControleRepository controleRepository;
    private final RapportControleRepository rapportControleRepository;
    private final ControleSalarieRepository controleSalarieRepository;
    private final ChantierRepository chantierRepository;
    private final ClientRepository clientRepository;
    private final EmailService emailService;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public Controle creer(UUID chantierId, UUID controleurUtilisateurId, LocalDate dateControle, String remarques,
                           UUID controleTiersId, LocalDate dateFin, boolean termine) {
        Controle controle = Controle.creer(chantierId, controleurUtilisateurId, dateControle, remarques,
            controleTiersId, dateFin, termine);
        return controleRepository.save(controle);
    }

    @Transactional(readOnly = true)
    public Controle obtenir(UUID id) {
        return controleRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrôle", id));
    }

    @Transactional(readOnly = true)
    public List<Controle> listerParChantier(UUID chantierId) {
        return controleRepository.findByChantierId(chantierId);
    }

    @Transactional(readOnly = true)
    public List<Controle> lister() {
        return controleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Controle> listerParControleTiers(UUID controleTiersId) {
        return controleRepository.findByControleTiersId(controleTiersId);
    }

    public Controle modifier(UUID id, LocalDate dateControle, String remarques, UUID controleTiersId,
                              LocalDate dateFin, boolean termine) {
        Controle controle = obtenir(id);
        controle.modifier(dateControle, remarques, controleTiersId, dateFin, termine);
        return controle;
    }

    public void supprimer(UUID id) {
        Controle controle = obtenir(id);
        controle.supprimer();
    }

    // --- Checklist salariés d'un contrôle ---
    // Verrouillée dès qu'un rapport a été généré : ses compteurs sont dérivés
    // de la checklist au moment de la génération, une modification après coup
    // les rendrait incohérents avec le rapport déjà émis (et potentiellement
    // déjà envoyé au client).

    private void verifierChecklistModifiable(UUID controleId) {
        if (rapportControleRepository.findByControleId(controleId).isPresent()) {
            throw new BusinessRuleViolationException(
                "Un rapport a déjà été généré pour ce contrôle : supprimez-le avant de modifier la checklist");
        }
    }

    public ControleSalarie ajouterSalarieAuControle(UUID controleId, UUID salarieId, UUID entrepriseId,
                                                      boolean accorde, UUID actionCorrectiveId) {
        obtenir(controleId); // valide l'existence du contrôle
        verifierChecklistModifiable(controleId);
        ControleSalarie entree = ControleSalarie.creer(controleId, salarieId, entrepriseId, accorde, actionCorrectiveId);
        return controleSalarieRepository.save(entree);
    }

    @Transactional(readOnly = true)
    public List<ControleSalarie> listerSalariesDuControle(UUID controleId) {
        return controleSalarieRepository.findByControleIdOrderByCreatedAtAsc(controleId);
    }

    public ControleSalarie modifierEntreeControleSalarie(UUID entreeId, boolean accorde, UUID actionCorrectiveId) {
        ControleSalarie entree = controleSalarieRepository.findById(entreeId)
            .orElseThrow(() -> new EntityNotFoundException("Entrée de checklist", entreeId));
        verifierChecklistModifiable(entree.getControleId());
        entree.modifier(accorde, actionCorrectiveId);
        return entree;
    }

    public void supprimerEntreeControleSalarie(UUID entreeId) {
        ControleSalarie entree = controleSalarieRepository.findById(entreeId)
            .orElseThrow(() -> new EntityNotFoundException("Entrée de checklist", entreeId));
        verifierChecklistModifiable(entree.getControleId());
        controleSalarieRepository.delete(entree);
    }

    // --- Rapports ---

    /** Compteurs dérivés de la checklist réellement saisie sur site — pas de ressaisie manuelle. */
    public RapportControle genererRapport(UUID controleId, int nbNouvellesEntreprises, int nbNouveauxSalaries,
                                           int nbSalariesDetaches, UUID responsableUtilisateurId) {
        obtenir(controleId);
        if (rapportControleRepository.findByControleId(controleId).isPresent()) {
            throw new BusinessRuleViolationException("Un rapport existe déjà pour ce contrôle");
        }
        List<ControleSalarie> entrees = controleSalarieRepository.findByControleIdOrderByCreatedAtAsc(controleId);
        int nbAccords = (int) entrees.stream().filter(ControleSalarie::isAccorde).count();
        int nbRefus = entrees.size() - nbAccords;
        int nbEntreprises = (int) entrees.stream().map(ControleSalarie::getEntrepriseId).distinct().count();
        RapportControle rapport = RapportControle.creer(controleId, entrees.size(), nbAccords, nbRefus,
            nbNouvellesEntreprises, nbNouveauxSalaries, nbEntreprises, nbSalariesDetaches, responsableUtilisateurId);
        return rapportControleRepository.save(rapport);
    }

    @Transactional(readOnly = true)
    public RapportControle obtenirRapport(UUID id) {
        return rapportControleRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Rapport", id));
    }

    @Transactional(readOnly = true)
    public List<RapportControle> listerRapports() {
        return rapportControleRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<RapportControle> listerRapportsParChantier(UUID chantierId) {
        List<UUID> controleIds = controleRepository.findByChantierId(chantierId).stream().map(Controle::getId).toList();
        return controleIds.isEmpty() ? List.of() : rapportControleRepository.findByControleIdInOrderByCreatedAtDesc(controleIds);
    }

    /** Restreint une liste de rapports à ceux dont le contrôle sous-jacent appartient à cet organisme de contrôle. */
    @Transactional(readOnly = true)
    public List<RapportControle> filtrerRapportsParControleTiers(List<RapportControle> rapports, UUID controleTiersId) {
        List<UUID> controleIds = rapports.stream().map(RapportControle::getControleId).distinct().toList();
        Set<UUID> controleIdsDuOrganisme = controleRepository.findAllById(controleIds).stream()
            .filter(c -> controleTiersId.equals(c.getControleTiersId()))
            .map(Controle::getId).collect(Collectors.toSet());
        return rapports.stream().filter(r -> controleIdsDuOrganisme.contains(r.getControleId())).toList();
    }

    public RapportControle modifierRapport(UUID id, int nbNouvellesEntreprises, int nbNouveauxSalaries,
                                            int nbSalariesDetaches, UUID responsableUtilisateurId) {
        RapportControle rapport = obtenirRapport(id);
        rapport.modifier(nbNouvellesEntreprises, nbNouveauxSalaries, nbSalariesDetaches, responsableUtilisateurId);
        return rapport;
    }

    public void supprimerRapport(UUID id) {
        obtenirRapport(id).supprimer();
    }

    public RapportControle envoyerRapport(UUID rapportId) {
        RapportControle rapport = obtenirRapport(rapportId);
        rapport.marquerEnvoye();
        return rapport;
    }

    /** Résout le chantier de chaque rapport (via son contrôle) en un seul aller-retour, pour éviter le N+1. */
    @Transactional(readOnly = true)
    public Map<UUID, UUID> resoudreChantierIdParRapport(List<RapportControle> rapports) {
        List<UUID> controleIds = rapports.stream().map(RapportControle::getControleId).distinct().toList();
        Map<UUID, UUID> chantierIdParControle = controleRepository.findAllById(controleIds).stream()
            .collect(Collectors.toMap(Controle::getId, Controle::getChantierId));
        return rapports.stream().collect(Collectors.toMap(RapportControle::getId,
            r -> chantierIdParControle.get(r.getControleId())));
    }

    public RapportControleResponse toResponse(RapportControle rapport) {
        UUID chantierId = obtenir(rapport.getControleId()).getChantierId();
        return RapportControleResponse.from(rapport, chantierId);
    }

    /** Renvoie le rapport par email au client du chantier concerné (adresse principale). */
    public void renvoyerRapportParEmail(UUID rapportId) {
        RapportControle rapport = obtenirRapport(rapportId);
        Controle controle = obtenir(rapport.getControleId());
        Chantier chantier = chantierRepository.findById(controle.getChantierId())
            .orElseThrow(() -> new EntityNotFoundException("Chantier", controle.getChantierId()));
        Client client = clientRepository.findById(chantier.getClientId())
            .orElseThrow(() -> new EntityNotFoundException("Client", chantier.getClientId()));
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            throw new BusinessRuleViolationException("Ce client n'a pas d'adresse email enregistrée");
        }
        String lien = "%s/controles/rapports/%s".formatted(frontendBaseUrl, rapportId);
        String sujet = "Rapport de contrôle — %s".formatted(chantier.getNom());
        String corps = """
            Bonjour,

            Le rapport de contrôle du chantier %s est disponible.

            Salariés contrôlés : %d
            Accords : %d
            Refus : %d
            Nouvelles entreprises : %d
            Nouveaux salariés : %d

            Consulter le rapport : %s

            Cordialement.
            """.formatted(chantier.getNom(), rapport.getNbSalariesControles(), rapport.getNbAccords(),
            rapport.getNbRefus(), rapport.getNbNouvellesEntreprises(), rapport.getNbNouveauxSalaries(), lien);
        emailService.envoyer(client.getEmail(), sujet, corps);
        rapport.marquerEnvoye();
    }
}
