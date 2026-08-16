package com.fluttiris.admincontrol.document.api.dto;

/**
 * Combien d'entreprises actives ont fourni ET fait valider tous leurs documents
 * obligatoires (voir DocumentService#calculerConformitePortefeuille) — sert au
 * KPI "Entreprises à jour" du tableau de bord Admin, plutôt qu'un total brut.
 */
public record ConformitePortefeuilleResponse(int conformes, int total) {
}
