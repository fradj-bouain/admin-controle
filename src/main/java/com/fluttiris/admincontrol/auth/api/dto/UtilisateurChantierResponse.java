package com.fluttiris.admincontrol.auth.api.dto;

import com.fluttiris.admincontrol.chantier.domain.StatutChantier;

import java.time.Instant;
import java.util.UUID;

/** Un chantier auquel ce compte a explicitement été assigné (voir chantier_utilisateur) —
    sert la fiche "détail équipe" du compte Client "accès total" : savoir, pour chaque
    responsable, sur quels chantiers il/elle intervient, depuis quand, et où. */
public record UtilisateurChantierResponse(UUID chantierId, String nom, String ville, StatutChantier statut, Instant depuisLe) {
}
