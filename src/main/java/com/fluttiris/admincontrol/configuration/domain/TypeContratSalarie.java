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
@Table(name = "type_contrat_salarie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TypeContratSalarie {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String libelle;

    public static TypeContratSalarie creer(String code, String libelle) {
        TypeContratSalarie typeContratSalarie = new TypeContratSalarie();
        typeContratSalarie.code = code;
        typeContratSalarie.libelle = libelle;
        return typeContratSalarie;
    }
}
