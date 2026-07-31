package com.fluttiris.admincontrol.messagerie.domain;

public enum TypeDeclencheur {
    /** N jours avant/après une date surveillée par un ChampSurveillable (voir ChampSurveillableRegistry). */
    CHAMP_SURVEILLABLE,
    /** Se déclenche une fois, immédiatement après la création d'un salarié. */
    CREATION_SALARIE,
    /** Se déclenche une fois, immédiatement après la création d'une entreprise. */
    CREATION_ENTREPRISE,
    /** Se déclenche une fois, immédiatement après l'affectation d'une entreprise à un chantier. */
    AFFECTATION_ENTREPRISE_CHANTIER,
    /** Récurrence pure, indépendante de toute entité : tous les nbJoursAvant jours. */
    PERIODIQUE,
    /** Ne se déclenche jamais automatiquement ; seulement via "Envoyer maintenant". */
    MANUEL
}
