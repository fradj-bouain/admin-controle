package com.fluttiris.admincontrol.messagerie.application;

import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierCreeeEvent;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseCreeeEvent;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifie;
import com.fluttiris.admincontrol.messagerie.domain.MessagePlanifieRepository;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisation;
import com.fluttiris.admincontrol.messagerie.domain.RegleAutomatisationRepository;
import com.fluttiris.admincontrol.messagerie.domain.TypeDeclencheur;
import com.fluttiris.admincontrol.salarie.domain.SalarieCreeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Déclenche les règles d'automatisation événementielles (création d'un
 * salarié/entreprise, affectation entreprise-chantier). N'écoute qu'APRÈS le
 * commit de la transaction qui a créé l'entité : une création annulée
 * (rollback) ne doit jamais générer de message.
 */
@Component
@RequiredArgsConstructor
public class AutomatisationEvenementListener {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RegleAutomatisationRepository regleAutomatisationRepository;
    private final MessagePlanifieRepository messagePlanifieRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ChantierRepository chantierRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSalarieCree(SalarieCreeEvent event) {
        Map<String, String> placeholders = placeholdersDeBase();
        entrepriseRepository.findById(event.entrepriseEmployeurId())
            .ifPresent(e -> placeholders.put("ENTREPRISE_NOM", e.getRaisonSociale()));
        genererPourType(TypeDeclencheur.CREATION_SALARIE, event.salarieId(), null, placeholders);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntrepriseCreee(EntrepriseCreeeEvent event) {
        Map<String, String> placeholders = placeholdersDeBase();
        entrepriseRepository.findById(event.entrepriseId())
            .ifPresent(e -> placeholders.put("ENTREPRISE_NOM", e.getRaisonSociale()));
        genererPourType(TypeDeclencheur.CREATION_ENTREPRISE, event.entrepriseId(), null, placeholders);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAffectationEntrepriseChantierCreee(AffectationEntrepriseChantierCreeeEvent event) {
        Map<String, String> placeholders = placeholdersDeBase();
        entrepriseRepository.findById(event.entrepriseId())
            .ifPresent(e -> placeholders.put("ENTREPRISE_NOM", e.getRaisonSociale()));
        chantierRepository.findById(event.chantierId())
            .ifPresent(c -> placeholders.put("CHANTIER_NOM", c.getNom()));
        genererPourType(TypeDeclencheur.AFFECTATION_ENTREPRISE_CHANTIER, event.affectationId(), event.chantierId(), placeholders);
    }

    private void genererPourType(TypeDeclencheur type, UUID sourceEntityId, UUID chantierId, Map<String, String> placeholders) {
        for (RegleAutomatisation regle : regleAutomatisationRepository.findByActifTrue()) {
            if (regle.getTypeDeclencheur() != type) {
                continue;
            }
            if (messagePlanifieRepository.existsByRegleIdAndSourceEntityId(regle.getId(), sourceEntityId)) {
                continue;
            }
            String sujet = substituer(regle.getSujet(), placeholders);
            String contenu = substituer(regle.getContenu(), placeholders);
            messagePlanifieRepository.save(
                MessagePlanifie.genererDepuisRegle(regle, sourceEntityId, chantierId, sujet, contenu, Instant.now()));
        }
    }

    private Map<String, String> placeholdersDeBase() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("DATE", LocalDate.now().format(FORMAT_DATE));
        return placeholders;
    }

    private String substituer(String texte, Map<String, String> placeholders) {
        String resultat = texte;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resultat = resultat.replace("[" + entry.getKey() + "]", entry.getValue());
        }
        return resultat;
    }
}
