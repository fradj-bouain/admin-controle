package com.fluttiris.admincontrol.entreprise.domain;

import java.util.UUID;

/** Publié après l'affectation d'une entreprise à un chantier — écouté par le moteur d'automatisation messagerie. */
public record AffectationEntrepriseChantierCreeeEvent(UUID affectationId, UUID entrepriseId, UUID chantierId) {
}
