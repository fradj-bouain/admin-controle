package com.fluttiris.admincontrol.configuration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "salarie_fonction")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalarieFonction {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String libelle;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static SalarieFonction creer(String libelle) {
        SalarieFonction fonction = new SalarieFonction();
        fonction.libelle = libelle;
        return fonction;
    }

    public void modifier(String libelle) {
        this.libelle = libelle;
    }

    public void supprimer() {
        this.deletedAt = Instant.now();
    }
}
