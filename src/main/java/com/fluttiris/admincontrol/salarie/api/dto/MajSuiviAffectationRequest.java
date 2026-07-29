package com.fluttiris.admincontrol.salarie.api.dto;

public record MajSuiviAffectationRequest(
    boolean epiGants,
    boolean epiCasque,
    boolean epiChaussures,
    boolean badgeEdite,
    boolean present
) {
}
