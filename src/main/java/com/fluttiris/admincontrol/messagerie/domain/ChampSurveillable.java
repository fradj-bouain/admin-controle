package com.fluttiris.admincontrol.messagerie.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Une source de données que l'automatisation messagerie peut surveiller pour
 * déclencher une relance N jours avant échéance (ex : "Document / Date
 * d'expiration"). Chaque implémentation est un bean Spring auto-enregistré
 * auprès de ChampSurveillableRegistry : ajouter une nouvelle source surveillable
 * (nouvelle entité, nouveau champ date) ne demande qu'une nouvelle classe,
 * jamais de migration ni de changement du moteur de planification.
 */
public interface ChampSurveillable {

    /** Identifiant stable, persisté sur RegleAutomatisation.champSurveillableId. */
    String getId();

    /** Libellé de l'entité source, pour affichage (ex : "Document"). */
    String getEntiteLibelle();

    /** Libellé du champ surveillé, pour affichage (ex : "Date d'expiration"). */
    String getChampLibelle();

    /** Toutes les occurrences dont la date surveillée tombe exactement sur dateCible. */
    List<EvenementDetecte> rechercher(LocalDate dateCible);
}
