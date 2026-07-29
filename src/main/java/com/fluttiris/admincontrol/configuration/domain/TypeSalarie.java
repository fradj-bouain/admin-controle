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
@Table(name = "type_salarie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TypeSalarie {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String libelle;

    public static TypeSalarie creer(String code, String libelle) {
        TypeSalarie typeSalarie = new TypeSalarie();
        typeSalarie.code = code;
        typeSalarie.libelle = libelle;
        return typeSalarie;
    }
}
