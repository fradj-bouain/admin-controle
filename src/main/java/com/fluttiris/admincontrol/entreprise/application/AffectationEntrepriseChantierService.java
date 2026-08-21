package com.fluttiris.admincontrol.entreprise.application;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierCreeeEvent;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierRepository;
import com.fluttiris.admincontrol.entreprise.domain.RoleEntreprise;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AffectationEntrepriseChantierService {

    private final AffectationEntrepriseChantierRepository affectationRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param affectationParenteId affectation (sur ce même chantier) de l'entreprise qui invite
     *                              ce sous-traitant ; null pour un rôle PRINCIPALE.
     */
    public AffectationEntrepriseChantier affecter(UUID chantierId, UUID entrepriseId, RoleEntreprise role,
                                                    UUID affectationParenteId) {
        affectationRepository.findByChantierIdAndEntrepriseIdAndRole(chantierId, entrepriseId, role)
            .ifPresent(existing -> {
                throw new BusinessRuleViolationException("Cette entreprise est déjà affectée à ce chantier avec ce rôle");
            });

        AffectationEntrepriseChantier parente = null;
        if (affectationParenteId != null) {
            parente = affectationRepository.findById(affectationParenteId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation parente", affectationParenteId));
            if (!parente.getChantierId().equals(chantierId)) {
                throw new BusinessRuleViolationException("L'affectation parente doit appartenir au même chantier");
            }
        }

        AffectationEntrepriseChantier affectation = AffectationEntrepriseChantier.creer(
            chantierId, entrepriseId, role, parente);
        affectation = affectationRepository.save(affectation);
        eventPublisher.publishEvent(new AffectationEntrepriseChantierCreeeEvent(
            affectation.getId(), affectation.getEntrepriseId(), affectation.getChantierId()));
        return affectation;
    }

    @Transactional(readOnly = true)
    public List<AffectationEntrepriseChantier> listerParChantier(UUID chantierId) {
        return affectationRepository.findByChantierId(chantierId);
    }

    @Transactional(readOnly = true)
    public List<AffectationEntrepriseChantier> listerParEntreprise(UUID entrepriseId) {
        return affectationRepository.findByEntrepriseId(entrepriseId);
    }

    /** Toutes les affectations, tous chantiers confondus — la liste transverse demandée
        (voir EntrepriseController, GET /entreprises/affectations). N'existait pas avant :
        chaque route existante exige un chantierId ou un entrepriseId précis en entrée. */
    @Transactional(readOnly = true)
    public List<AffectationEntrepriseChantier> listerToutes() {
        return affectationRepository.findAll();
    }

    /** Résout entrepriseId pour un lot d'affectations entreprise↔chantier, en une seule
        requête — version "batch" de entrepriseIdDeLAffectation, pour ne jamais appeler ce
        dernier dans une boucle (voir SalarieController.listerAffectations, N+1 évité). */
    @Transactional(readOnly = true)
    public Map<UUID, UUID> entrepriseIdParAffectationIds(List<UUID> affectationIds) {
        if (affectationIds.isEmpty()) {
            return Map.of();
        }
        return affectationRepository.findAllById(affectationIds.stream().distinct().toList()).stream()
            .collect(Collectors.toMap(AffectationEntrepriseChantier::getId, AffectationEntrepriseChantier::getEntrepriseId));
    }

    /** Email de contact propre à cette relation (entreprise, chantier) — voir
        AffectationEntrepriseChantier.emailContact. Stocké pour l'instant, pas encore
        utilisé pour router un envoi réel (voir MessageService.envoyer). */
    public AffectationEntrepriseChantier modifierEmailContact(UUID affectationId, String emailContact) {
        AffectationEntrepriseChantier affectation = affectationRepository.findById(affectationId)
            .orElseThrow(() -> new EntityNotFoundException("Affectation", affectationId));
        affectation.modifierEmailContact(emailContact);
        return affectation;
    }

    /**
     * Désactive l'entreprise sur ce chantier ET, en cascade, tous les sous-traitants
     * qu'elle avait elle-même invités sur ce même chantier (une STT1 désactivée ne
     * peut pas laisser ses STT2 actifs "orphelins" sur le chantier).
     */
    public void desactiver(UUID affectationId) {
        AffectationEntrepriseChantier affectation = affectationRepository.findById(affectationId)
            .orElseThrow(() -> new EntityNotFoundException("Affectation", affectationId));

        affectation.desactiver();

        affectationRepository.findByAffectationParenteId(affectationId).stream()
            .filter(AffectationEntrepriseChantier::estActive)
            .forEach(enfant -> desactiver(enfant.getId()));
    }

    /** Volontairement sans cascade (voir AffectationEntrepriseChantier.reactiver()) : un
        sous-traitant désactivé en même temps que son parent ne revient pas automatiquement,
        l'ADMIN le réactive lui-même s'il le souhaite. */
    public void reactiver(UUID affectationId) {
        AffectationEntrepriseChantier affectation = affectationRepository.findById(affectationId)
            .orElseThrow(() -> new EntityNotFoundException("Affectation", affectationId));
        affectation.reactiver();
    }

    /**
     * Suppression logique, avec la même cascade que {@link #desactiver(UUID)} :
     * un sous-traitant ne peut pas rester visible sous une affectation supprimée.
     */
    public void supprimer(UUID affectationId) {
        AffectationEntrepriseChantier affectation = affectationRepository.findById(affectationId)
            .orElseThrow(() -> new EntityNotFoundException("Affectation", affectationId));

        affectationRepository.findByAffectationParenteId(affectationId)
            .forEach(enfant -> supprimer(enfant.getId()));

        affectation.supprimer();
    }
}
