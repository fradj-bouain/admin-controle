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
@Table(name = "pays")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pays {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "code_iso", nullable = false, unique = true)
    private String codeIso;

    @Column(nullable = false)
    private String nom;

    private String zone;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static Pays creer(String codeIso, String nom, String zone) {
        Pays pays = new Pays();
        pays.codeIso = codeIso;
        pays.nom = nom;
        pays.zone = zone;
        return pays;
    }

    public void modifier(String codeIso, String nom, String zone) {
        this.codeIso = codeIso;
        this.nom = nom;
        this.zone = zone;
    }

    public void supprimer() {
        this.deletedAt = Instant.now();
    }
}
