package com.fluttiris.admincontrol.controle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.time.Instant;
import java.util.UUID;

/** Une ligne de la checklist d'un contrôle : un salarié passé en revue sur site, avec son accord/refus. */
@Entity
@Table(name = "controle_salarie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ControleSalarie {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "controle_id", nullable = false)
    private UUID controleId;

    @Column(name = "salarie_id", nullable = false)
    private UUID salarieId;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(nullable = false)
    private boolean accorde;

    /** Motif du refus, choisi dans le catalogue Actions correctives — null si accordé. */
    @Column(name = "action_corrective_id")
    private UUID actionCorrectiveId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ControleSalarie creer(UUID controleId, UUID salarieId, UUID entrepriseId, boolean accorde, UUID actionCorrectiveId) {
        ControleSalarie entree = new ControleSalarie();
        entree.controleId = controleId;
        entree.salarieId = salarieId;
        entree.entrepriseId = entrepriseId;
        entree.accorde = accorde;
        entree.actionCorrectiveId = accorde ? null : actionCorrectiveId;
        return entree;
    }

    public void modifier(boolean accorde, UUID actionCorrectiveId) {
        this.accorde = accorde;
        this.actionCorrectiveId = accorde ? null : actionCorrectiveId;
    }
}
