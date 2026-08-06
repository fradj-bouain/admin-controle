package com.fluttiris.admincontrol.document.api.dto;

import java.time.LocalDate;

/**
 * Dates saisies par l'administrateur au moment de la validation — jamais fournies par
 * l'entreprise au dépôt (voir CreateDocumentRequest, ignorées si non-admin côté contrôleur).
 */
public record ValiderDocumentRequest(LocalDate dateDebutValidite, LocalDate dateExpiration) {
}
