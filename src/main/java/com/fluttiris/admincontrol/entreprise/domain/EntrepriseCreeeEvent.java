package com.fluttiris.admincontrol.entreprise.domain;

import java.util.UUID;

/** Publié après la création d'une entreprise — écouté par le moteur d'automatisation messagerie. */
public record EntrepriseCreeeEvent(UUID entrepriseId) {
}
