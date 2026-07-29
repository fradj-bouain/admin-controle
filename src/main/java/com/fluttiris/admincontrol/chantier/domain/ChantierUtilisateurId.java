package com.fluttiris.admincontrol.chantier.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChantierUtilisateurId implements Serializable {
    private UUID chantierId;
    private UUID utilisateurId;
}
