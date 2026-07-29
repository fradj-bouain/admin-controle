package com.fluttiris.admincontrol.salarie.application;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierRepository;
import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantier;
import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AffectationSalarieChantierService {

    private final AffectationSalarieChantierRepository affectationSalarieRepository;
    private final AffectationEntrepriseChantierRepository affectationEntrepriseRepository;

    /**
     * @param affectationEntrepriseChantierId l'entreprise (affectée à CE chantier) pour laquelle
     *                                         le salarié intervient. Doit être une affectation
     *                                         active sur le même chantier — jamais une entreprise
     *                                         qui n'a pas été affectée au chantier en question.
     */
    public AffectationSalarieChantier affecter(UUID salarieId, UUID chantierId, UUID affectationEntrepriseChantierId) {
        var affectationEntreprise = affectationEntrepriseRepository.findById(affectationEntrepriseChantierId)
            .orElseThrow(() -> new EntityNotFoundException("Affectation entreprise", affectationEntrepriseChantierId));

        if (!affectationEntreprise.getChantierId().equals(chantierId)) {
            throw new BusinessRuleViolationException(
                "L'entreprise choisie n'est pas affectée à ce chantier");
        }
        if (!affectationEntreprise.estActive()) {
            throw new BusinessRuleViolationException(
                "L'entreprise choisie n'est plus active sur ce chantier");
        }
        if (affectationSalarieRepository.existsBySalarieIdAndChantierIdAndDateFinIsNull(salarieId, chantierId)) {
            throw new BusinessRuleViolationException(
                "Ce salarié a déjà une affectation active sur ce chantier");
        }

        AffectationSalarieChantier affectation = AffectationSalarieChantier.creer(
            salarieId, chantierId, affectationEntrepriseChantierId);
        return affectationSalarieRepository.save(affectation);
    }

    @Transactional(readOnly = true)
    public List<AffectationSalarieChantier> listerParChantier(UUID chantierId) {
        return affectationSalarieRepository.findByChantierId(chantierId);
    }

    @Transactional(readOnly = true)
    public List<AffectationSalarieChantier> listerParSalarie(UUID salarieId) {
        return affectationSalarieRepository.findBySalarieId(salarieId);
    }

    public AffectationSalarieChantier accorderAcces(UUID affectationId) {
        AffectationSalarieChantier affectation = obtenir(affectationId);
        affectation.accorderAcces();
        return affectation;
    }

    public AffectationSalarieChantier refuserAcces(UUID affectationId, String motif) {
        AffectationSalarieChantier affectation = obtenir(affectationId);
        affectation.refuserAcces(motif);
        return affectation;
    }

    public AffectationSalarieChantier cloturer(UUID affectationId) {
        AffectationSalarieChantier affectation = obtenir(affectationId);
        affectation.cloturer();
        return affectation;
    }

    public void supprimer(UUID affectationId) {
        AffectationSalarieChantier affectation = obtenir(affectationId);
        affectation.supprimer();
    }

    public AffectationSalarieChantier majSuivi(UUID affectationId, boolean epiGants, boolean epiCasque,
                                                boolean epiChaussures, boolean badgeEdite, boolean present) {
        AffectationSalarieChantier affectation = obtenir(affectationId);
        affectation.majSuivi(epiGants, epiCasque, epiChaussures, badgeEdite, present);
        return affectation;
    }

    private AffectationSalarieChantier obtenir(UUID id) {
        return affectationSalarieRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Affectation salarié", id));
    }
}
