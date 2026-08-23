package com.fluttiris.admincontrol.entreprise.api.dto;

public record ModifierCoordonneesContactRequest(
    String emailContact,
    String telephoneContact,
    String adresseContact
) {
}
