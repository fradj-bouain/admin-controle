package com.fluttiris.admincontrol.qrcode.api.dto;

import com.fluttiris.admincontrol.qrcode.application.QrCodeService;

import java.util.UUID;

public record VerificationResponse(boolean valide, String nom, String prenom, UUID entrepriseId) {
    public static VerificationResponse from(QrCodeService.VerificationResult result) {
        return new VerificationResponse(result.valide(), result.nom(), result.prenom(), result.entrepriseId());
    }
}
