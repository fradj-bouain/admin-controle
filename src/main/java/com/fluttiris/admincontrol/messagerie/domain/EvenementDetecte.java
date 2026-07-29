package com.fluttiris.admincontrol.messagerie.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Une occurrence concrète d'un {@link ChampSurveillable} arrivée à échéance
 * (ex : ce document précis expire à la date ciblée). sourceEntityId sert de
 * clé de dédoublonnage (voir MessagePlanifieRepository.existsByRegleIdAndSourceEntityId) ;
 * placeholders alimente la substitution [DATE]/[CHANTIER_NOM]/... dans le message.
 */
public record EvenementDetecte(UUID sourceEntityId, UUID chantierId, Map<String, String> placeholders) {
}
