package com.fluttiris.admincontrol.salarie.application;

import com.fluttiris.admincontrol.chantier.application.ChantierService;
import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantier;
import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantierRepository;
import com.fluttiris.admincontrol.salarie.domain.Salarie;
import com.fluttiris.admincontrol.salarie.domain.SalarieCreeEvent;
import com.fluttiris.admincontrol.salarie.domain.SalarieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SalarieService {

    private final SalarieRepository salarieRepository;
    private final ChantierService chantierService;
    private final ChantierRepository chantierRepository;
    private final AffectationSalarieChantierRepository affectationSalarieChantierRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Salarie creer(String nom, String prenom, LocalDate dateNaissance, UUID nationalitePaysId,
                          UUID entrepriseEmployeurId, UUID typeSalarieId, UUID typeContratId, UUID fonctionId) {
        Salarie salarie = Salarie.creer(nom, prenom, dateNaissance, nationalitePaysId, entrepriseEmployeurId,
            typeSalarieId, typeContratId, fonctionId);
        salarie = salarieRepository.save(salarie);
        eventPublisher.publishEvent(new SalarieCreeEvent(salarie.getId(), salarie.getEntrepriseEmployeurId()));
        return salarie;
    }

    public Salarie modifier(UUID id, String nom, String prenom, LocalDate dateNaissance, UUID nationalitePaysId,
                             UUID entrepriseEmployeurId, UUID typeSalarieId, UUID typeContratId, UUID fonctionId) {
        Salarie salarie = obtenir(id);
        salarie.modifier(nom, prenom, dateNaissance, nationalitePaysId, entrepriseEmployeurId, typeSalarieId,
            typeContratId, fonctionId);
        return salarie;
    }

    @Transactional(readOnly = true)
    public Salarie obtenir(UUID id) {
        return salarieRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Salarié", id));
    }

    @Transactional(readOnly = true)
    public List<Salarie> lister(UUID entrepriseId) {
        return entrepriseId != null
            ? salarieRepository.findByEntrepriseEmployeurId(entrepriseId)
            : salarieRepository.findAll();
    }

    /** Salariés affectés à un chantier accessible par ce client (tous ses chantiers, ou
        seulement ceux assignés à cet utilisateur — voir ChantierService), via leurs
        affectations chantier. */
    @Transactional(readOnly = true)
    public List<Salarie> listerParClient(UUID clientId, UUID utilisateurId) {
        List<UUID> chantierIds = chantierService.listerIdsAccessiblesPourClient(clientId, utilisateurId);
        if (chantierIds.isEmpty()) {
            return List.of();
        }
        List<UUID> salarieIds = affectationSalarieChantierRepository.findByChantierIdIn(chantierIds).stream()
            .map(AffectationSalarieChantier::getSalarieId).distinct().toList();
        return salarieRepository.findAllById(salarieIds);
    }

    public Salarie desactiver(UUID id) {
        Salarie salarie = obtenir(id);
        salarie.desactiver();
        return salarie;
    }

    public Salarie activer(UUID id) {
        Salarie salarie = obtenir(id);
        salarie.activer();
        return salarie;
    }

    public void supprimer(UUID id) {
        Salarie salarie = obtenir(id);
        salarie.supprimer();
    }

    /**
     * Chantier "en cours" (affectation sans date de fin) pour chaque salarié du lot —
     * à ne pas confondre avec le statut Actif/Inactif du salarié (simple interrupteur
     * administratif, sans lien avec une affectation chantier réelle ; voir audit UX).
     * Un salarié absent du résultat n'a aucune affectation en cours ("Disponible" côté
     * affichage — décision volontairement laissée au frontend, pas ici). En une seule
     * requête batch, pas de N+1 sur la liste complète.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> chantierActuelParSalarie(List<UUID> salarieIds) {
        if (salarieIds.isEmpty()) {
            return Map.of();
        }
        List<AffectationSalarieChantier> enCours = affectationSalarieChantierRepository
            .findBySalarieIdInAndDateFinIsNull(salarieIds);
        if (enCours.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> nomChantierParId = chantierRepository.findAllById(
            enCours.stream().map(AffectationSalarieChantier::getChantierId).distinct().toList()
        ).stream().collect(Collectors.toMap(Chantier::getId, Chantier::getNom));

        Map<UUID, List<String>> nomsParSalarie = new HashMap<>();
        for (AffectationSalarieChantier a : enCours) {
            nomsParSalarie.computeIfAbsent(a.getSalarieId(), k -> new ArrayList<>())
                .add(nomChantierParId.getOrDefault(a.getChantierId(), ""));
        }
        Map<UUID, String> resultat = new HashMap<>();
        for (var entry : nomsParSalarie.entrySet()) {
            List<String> noms = entry.getValue();
            resultat.put(entry.getKey(), noms.size() == 1 ? noms.get(0) : noms.size() + " chantiers en cours");
        }
        return resultat;
    }
}
