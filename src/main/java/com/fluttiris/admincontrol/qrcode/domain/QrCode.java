package com.fluttiris.admincontrol.qrcode.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Un seul QR code actif par salarié (contrainte unique sur salarie_id) : la
 * régénération remplace la valeur en place plutôt que de créer une nouvelle
 * ligne, ce qui invalide immédiatement l'ancien code — condition nécessaire
 * pour empêcher qu'un badge perdu/partagé reste utilisable.
 */
@Entity
@Table(name = "qr_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QrCode {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "salarie_id", nullable = false, unique = true)
    private UUID salarieId;

    @Column(name = "code_valeur", nullable = false, unique = true)
    private String codeValeur;

    @Column(name = "carte_identite_numerique_url")
    private String carteIdentiteNumeriqueUrl;

    @Column(nullable = false)
    private boolean actif = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static QrCode creer(UUID salarieId) {
        QrCode qrCode = new QrCode();
        qrCode.salarieId = salarieId;
        qrCode.codeValeur = genererValeur();
        return qrCode;
    }

    /** Invalide l'ancien code et en émet un nouveau : l'ancien QR imprimé/partagé cesse de fonctionner. */
    public void regenerer() {
        this.codeValeur = genererValeur();
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    private static String genererValeur() {
        return UUID.randomUUID().toString();
    }
}
