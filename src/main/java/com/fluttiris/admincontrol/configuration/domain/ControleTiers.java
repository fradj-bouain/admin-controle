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
@Table(name = "controle_tiers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ControleTiers {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nom;

    public static ControleTiers creer(String nom) {
        ControleTiers controleTiers = new ControleTiers();
        controleTiers.nom = nom;
        return controleTiers;
    }

    public void modifier(String nom) {
        this.nom = nom;
    }
}
