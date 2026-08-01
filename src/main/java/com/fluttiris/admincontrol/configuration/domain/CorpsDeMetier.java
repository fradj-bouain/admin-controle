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
@Table(name = "corps_de_metier")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorpsDeMetier {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String libelle;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static CorpsDeMetier creer(String libelle) {
        CorpsDeMetier corpsDeMetier = new CorpsDeMetier();
        corpsDeMetier.libelle = libelle;
        return corpsDeMetier;
    }

    public void modifier(String libelle) {
        this.libelle = libelle;
    }

    public void supprimer() {
        this.deletedAt = Instant.now();
    }
}
