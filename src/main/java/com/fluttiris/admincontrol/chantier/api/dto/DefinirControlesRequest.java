package com.fluttiris.admincontrol.chantier.api.dto;

import com.fluttiris.admincontrol.chantier.domain.RecurrenceControles;

import java.time.LocalDate;

public record DefinirControlesRequest(
    RecurrenceControles recurrenceControles,
    LocalDate dateProchainControle
) {
}
