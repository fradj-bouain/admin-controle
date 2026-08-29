package com.fluttiris.admincontrol.messagerie.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "expediteur_utilisateur_id", nullable = false)
    private UUID expediteurUtilisateurId;

    @Column(name = "chantier_id")
    private UUID chantierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinataire_type", nullable = false)
    private DestinataireType destinataireType;

    @Column(name = "destinataire_id", nullable = false)
    private UUID destinataireId;

    @Column(nullable = false)
    private String sujet;

    @Column(nullable = false)
    private String contenu;

    /** Documents attendus si ce message est une "demande de document(s)" (voir salarie/entreprise-detail
        demanderDocuments) — permet au destinataire de déposer directement, depuis le message, un
        fichier pour chacun. Peut viser un seul document (cas historique) ou plusieurs à la fois
        (sélection groupée, un seul message au lieu d'un par document). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_type_document", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "type_document_id")
    private List<UUID> typeDocumentIds = new ArrayList<>();

    /** Renseigné seulement quand la demande vise le document d'un salarié précis
        (sinon le document attendu est celui de l'entreprise destinataire elle-même). */
    @Column(name = "salarie_id")
    private UUID salarieId;

    @Column(nullable = false)
    private boolean lu = false;

    @Column(name = "lu_par_utilisateur_id")
    private UUID luParUtilisateurId;

    @Column(name = "date_lu")
    private Instant dateLu;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Message envoyer(UUID expediteurUtilisateurId, UUID chantierId, DestinataireType destinataireType,
                                   UUID destinataireId, String sujet, String contenu) {
        return envoyer(expediteurUtilisateurId, chantierId, destinataireType, destinataireId, sujet, contenu, null, null);
    }

    public static Message envoyer(UUID expediteurUtilisateurId, UUID chantierId, DestinataireType destinataireType,
                                   UUID destinataireId, String sujet, String contenu, List<UUID> typeDocumentIds, UUID salarieId) {
        Message message = new Message();
        message.expediteurUtilisateurId = expediteurUtilisateurId;
        message.chantierId = chantierId;
        message.destinataireType = destinataireType;
        message.destinataireId = destinataireId;
        message.sujet = sujet;
        message.contenu = contenu;
        message.typeDocumentIds = typeDocumentIds != null ? typeDocumentIds : new ArrayList<>();
        message.salarieId = salarieId;
        return message;
    }

    public void marquerLu(UUID utilisateurId) {
        this.lu = true;
        this.luParUtilisateurId = utilisateurId;
        this.dateLu = Instant.now();
    }
}
