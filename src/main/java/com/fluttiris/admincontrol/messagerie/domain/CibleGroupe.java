package com.fluttiris.admincontrol.messagerie.domain;

public enum CibleGroupe {
    SPECIFIQUE,
    TOUS_UTILISATEURS,
    TOUS_CLIENTS,
    TOUTES_ENTREPRISES,
    /** Pas de compte/boîte de réception propre aux salariés dans cette appli :
     *  route vers l'entreprise employeuse des salariés actifs concernés. */
    TOUS_SALARIES
}
