package com.fluttiris.admincontrol.configuration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_corrective")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionCorrective {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CibleActionCorrective cible;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static ActionCorrective creer(String nom, CibleActionCorrective cible) {
        ActionCorrective action = new ActionCorrective();
        action.nom = nom;
        action.cible = cible;
        return action;
    }

    public void modifier(String nom, CibleActionCorrective cible) {
        this.nom = nom;
        this.cible = cible;
    }

    public void supprimer() {
        this.deletedAt = Instant.now();
    }
}
