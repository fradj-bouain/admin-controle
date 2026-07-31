package com.fluttiris.admincontrol.salarie.domain;

import java.util.UUID;

/** Publié après la création d'un salarié — écouté par le moteur d'automatisation messagerie. */
public record SalarieCreeEvent(UUID salarieId, UUID entrepriseEmployeurId) {
}
