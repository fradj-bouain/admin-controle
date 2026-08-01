package com.fluttiris.admincontrol.messagerie.domain;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_planifie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MessagePlanifie {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "regle_id")
    private UUID regleId;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @Column(name = "expediteur_utilisateur_id")
    private UUID expediteurUtilisateurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cible_groupe", nullable = false)
    private CibleGroupe cibleGroupe;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinataire_type")
    private DestinataireType destinataireType;

    @Column(name = "destinataire_id")
    private UUID destinataireId;

    @Column(name = "chantier_id")
    private UUID chantierId;

    @Column(nullable = false)
    private String sujet;

    @Column(nullable = false)
    private String contenu;

    @Column(name = "date_envoi_prevue", nullable = false)
    private Instant dateEnvoiPrevue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMessagePlanifie statut = StatutMessagePlanifie.EN_ATTENTE;

    @Column(name = "date_envoi_reelle")
    private Instant dateEnvoiReelle;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static MessagePlanifie planifier(UUID expediteurUtilisateurId, CibleGroupe cibleGroupe,
                                             DestinataireType destinataireType, UUID destinataireId, UUID chantierId,
                                             String sujet, String contenu, Instant dateEnvoiPrevue) {
        valider(cibleGroupe, destinataireType, destinataireId);
        MessagePlanifie message = new MessagePlanifie();
        message.expediteurUtilisateurId = expediteurUtilisateurId;
        message.cibleGroupe = cibleGroupe;
        message.destinataireType = destinataireType;
        message.destinataireId = destinataireId;
        message.chantierId = chantierId;
        message.sujet = sujet;
        message.contenu = contenu;
        message.dateEnvoiPrevue = dateEnvoiPrevue;
        return message;
    }

    public static MessagePlanifie genererDepuisRegle(RegleAutomatisation regle, UUID sourceEntityId, UUID chantierId,
                                                       String sujet, String contenu, Instant dateEnvoiPrevue) {
        MessagePlanifie message = new MessagePlanifie();
        message.regleId = regle.getId();
        message.sourceEntityId = sourceEntityId;
        message.expediteurUtilisateurId = regle.getCreatedBy();
        message.cibleGroupe = regle.getCibleGroupe();
        message.destinataireType = regle.getDestinataireType();
        message.destinataireId = regle.getDestinataireId();
        message.chantierId = chantierId;
        message.sujet = sujet;
        message.contenu = contenu;
        message.dateEnvoiPrevue = dateEnvoiPrevue;
        return message;
    }

    /**
     * Notification manuelle envoyée immédiatement depuis la fiche d'un document
     * (bouton "Notifier par email") — contrairement à {@link #planifier}/{@link
     * #genererDepuisRegle}, elle est créée déjà au statut ENVOYE (l'email part au
     * même moment) et porte sourceEntityId pour apparaître dans l'historique des
     * relances du document/salarié concerné.
     */
    public static MessagePlanifie manuelle(UUID expediteurUtilisateurId, UUID sourceEntityId,
                                            DestinataireType destinataireType, UUID destinataireId,
                                            String sujet, String contenu) {
        MessagePlanifie message = new MessagePlanifie();
        message.sourceEntityId = sourceEntityId;
        message.expediteurUtilisateurId = expediteurUtilisateurId;
        message.cibleGroupe = CibleGroupe.SPECIFIQUE;
        message.destinataireType = destinataireType;
        message.destinataireId = destinataireId;
        message.sujet = sujet;
        message.contenu = contenu;
        message.dateEnvoiPrevue = Instant.now();
        message.marquerEnvoye();
        return message;
    }

    public void marquerEnvoye() {
        this.statut = StatutMessagePlanifie.ENVOYE;
        this.dateEnvoiReelle = Instant.now();
    }

    public void annuler() {
        if (this.statut != StatutMessagePlanifie.EN_ATTENTE) {
            throw new BusinessRuleViolationException("Seul un message en attente peut être annulé");
        }
        this.statut = StatutMessagePlanifie.ANNULE;
    }

    private static void valider(CibleGroupe cibleGroupe, DestinataireType destinataireType, UUID destinataireId) {
        boolean destinataireRenseigne = destinataireType != null && destinataireId != null;
        if (cibleGroupe == CibleGroupe.SPECIFIQUE && !destinataireRenseigne) {
            throw new BusinessRuleViolationException(
                "Une planification ciblant un destinataire spécifique doit préciser le type et l'identifiant du destinataire");
        }
        if (cibleGroupe != CibleGroupe.SPECIFIQUE && destinataireRenseigne) {
            throw new BusinessRuleViolationException(
                "Une planification ciblant un groupe ne doit pas préciser de destinataire spécifique");
        }
    }
}
