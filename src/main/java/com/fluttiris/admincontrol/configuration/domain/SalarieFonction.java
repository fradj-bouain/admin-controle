package com.fluttiris.admincontrol.configuration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "salarie_fonction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalarieFonction {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String libelle;

    public static SalarieFonction creer(String libelle) {
        SalarieFonction fonction = new SalarieFonction();
        fonction.libelle = libelle;
        return fonction;
    }
}
