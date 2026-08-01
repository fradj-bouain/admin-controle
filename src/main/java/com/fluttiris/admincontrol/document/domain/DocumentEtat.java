package com.fluttiris.admincontrol.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Motif de refus paramétrable d'un document (ex : "Attestation expirée",
 * "Erreur nom/prénom") — remplace le texte libre lors d'un refus, comme sur
 * le site legacy. Suppression logique (deleted_at) comme les autres référentiels.
 */
@Entity
@Table(name = "document_etat")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DocumentEtat {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String titre;

    @Column(name = "par_defaut", nullable = false)
    private boolean parDefaut = false;

    /** Ce motif signifie-t-il "document arrivé à expiration" ? */
    @Column(name = "date_expiree", nullable = false)
    private boolean dateExpiree = false;

    /** Choisir ce motif valide-t-il le document (par opposition à un motif de refus) ? */
    @Column(name = "valide_le_document", nullable = false)
    private boolean valideLeDocument = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static DocumentEtat creer(String titre, boolean parDefaut, boolean dateExpiree, boolean valideLeDocument) {
        DocumentEtat etat = new DocumentEtat();
        etat.titre = titre;
        etat.parDefaut = parDefaut;
        etat.dateExpiree = dateExpiree;
        etat.valideLeDocument = valideLeDocument;
        return etat;
    }

    public void modifier(String titre, boolean parDefaut, boolean dateExpiree, boolean valideLeDocument) {
        this.titre = titre;
        this.parDefaut = parDefaut;
        this.dateExpiree = dateExpiree;
        this.valideLeDocument = valideLeDocument;
    }

    public void supprimer() {
        this.deletedAt = Instant.now();
    }
}
