package com.fluttiris.admincontrol.salarie.application;

import com.fluttiris.admincontrol.chantier.application.ChantierService;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SalarieService {

    private final SalarieRepository salarieRepository;
    private final ChantierService chantierService;
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
}
