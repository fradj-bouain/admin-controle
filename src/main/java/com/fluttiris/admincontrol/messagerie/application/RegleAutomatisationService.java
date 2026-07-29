package com.fluttiris.admincontrol.messagerie.application;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.messagerie.domain.CibleGroupe;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisation;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RegleAutomatisationService {

    private final RegleAutomatisationRepository regleAutomatisationRepository;
    private final ChampSurveillableRegistry champSurveillableRegistry;

    public RegleAutomatisation creer(String nom, String champSurveillableId, int nbJoursAvant,
                                      CibleGroupe cibleGroupe, DestinataireType destinataireType,
                                      UUID destinataireId, String sujet, String contenu) {
        verifierChampSurveillable(champSurveillableId);
        RegleAutomatisation regle = RegleAutomatisation.creer(nom, champSurveillableId, nbJoursAvant, cibleGroupe,
            destinataireType, destinataireId, sujet, contenu);
        return regleAutomatisationRepository.save(regle);
    }

    public RegleAutomatisation modifier(UUID id, String nom, String champSurveillableId,
                                         int nbJoursAvant, CibleGroupe cibleGroupe, DestinataireType destinataireType,
                                         UUID destinataireId, String sujet, String contenu) {
        verifierChampSurveillable(champSurveillableId);
        RegleAutomatisation regle = obtenir(id);
        regle.modifier(nom, champSurveillableId, nbJoursAvant, cibleGroupe, destinataireType, destinataireId, sujet, contenu);
        return regle;
    }

    @Transactional(readOnly = true)
    public RegleAutomatisation obtenir(UUID id) {
        return regleAutomatisationRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Règle d'automatisation", id));
    }

    @Transactional(readOnly = true)
    public List<RegleAutomatisation> lister() {
        return regleAutomatisationRepository.findAll();
    }

    public RegleAutomatisation activer(UUID id) {
        RegleAutomatisation regle = obtenir(id);
        regle.activer();
        return regle;
    }

    public RegleAutomatisation desactiver(UUID id) {
        RegleAutomatisation regle = obtenir(id);
        regle.desactiver();
        return regle;
    }

    public void supprimer(UUID id) {
        obtenir(id).supprimer();
    }

    private void verifierChampSurveillable(String champSurveillableId) {
        if (champSurveillableRegistry.parId(champSurveillableId).isEmpty()) {
            throw new BusinessRuleViolationException("Champ surveillable inconnu : " + champSurveillableId);
        }
    }
}
