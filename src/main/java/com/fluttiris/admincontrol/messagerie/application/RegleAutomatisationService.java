package com.fluttiris.admincontrol.messagerie.application;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.messagerie.domain.CibleGroupe;
import com.fluttiris.admincontrol.messagerie.domain.DestinataireType;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifieRepository;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisation;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisationRepository;
import com.fluttiris.admincontrol.messagerie.domain.TypeDeclencheur;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RegleAutomatisationService {

    private final RegleAutomatisationRepository regleAutomatisationRepository;
    private final MessagePlanifieRepository messagePlanifieRepository;
    private final ChampSurveillableRegistry champSurveillableRegistry;
    private final AutomatisationSchedulerService automatisationSchedulerService;

    public RegleAutomatisation creer(String nom, TypeDeclencheur typeDeclencheur, String champSurveillableId,
                                      int nbJoursAvant, CibleGroupe cibleGroupe, DestinataireType destinataireType,
                                      UUID destinataireId, String sujet, String contenu, String numeroInterne,
                                      String titreInterne) {
        verifierChampSurveillable(typeDeclencheur, champSurveillableId);
        RegleAutomatisation regle = RegleAutomatisation.creer(nom, typeDeclencheur, champSurveillableId, nbJoursAvant,
            cibleGroupe, destinataireType, destinataireId, sujet, contenu, numeroInterne, titreInterne);
        return regleAutomatisationRepository.save(regle);
    }

    public RegleAutomatisation modifier(UUID id, String nom, TypeDeclencheur typeDeclencheur, String champSurveillableId,
                                         int nbJoursAvant, CibleGroupe cibleGroupe, DestinataireType destinataireType,
                                         UUID destinataireId, String sujet, String contenu, String numeroInterne,
                                         String titreInterne) {
        verifierChampSurveillable(typeDeclencheur, champSurveillableId);
        RegleAutomatisation regle = obtenir(id);
        regle.modifier(nom, typeDeclencheur, champSurveillableId, nbJoursAvant, cibleGroupe, destinataireType,
            destinataireId, sujet, contenu, numeroInterne, titreInterne);
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

    /** Génère et envoie immédiatement un message pour cette règle, sans attendre son déclencheur habituel. */
    public void envoyerMaintenant(UUID id) {
        RegleAutomatisation regle = obtenir(id);
        MessagePlanifie message = MessagePlanifie.genererDepuisRegle(regle, null, null, regle.getSujet(),
            regle.getContenu(), Instant.now());
        message = messagePlanifieRepository.save(message);
        automatisationSchedulerService.envoyerUnMessagePlanifie(message);
    }

    private void verifierChampSurveillable(TypeDeclencheur typeDeclencheur, String champSurveillableId) {
        if (typeDeclencheur == TypeDeclencheur.CHAMP_SURVEILLABLE && champSurveillableRegistry.parId(champSurveillableId).isEmpty()) {
            throw new BusinessRuleViolationException("Champ surveillable inconnu : " + champSurveillableId);
        }
    }
}
