package com.fluttiris.admincontrol.messagerie.domain;

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
        Message message = new Message();
        message.expediteurUtilisateurId = expediteurUtilisateurId;
        message.chantierId = chantierId;
        message.destinataireType = destinataireType;
        message.destinataireId = destinataireId;
        message.sujet = sujet;
        message.contenu = contenu;
        return message;
    }

    public void marquerLu(UUID utilisateurId) {
        this.lu = true;
        this.luParUtilisateurId = utilisateurId;
        this.dateLu = Instant.now();
    }
}
