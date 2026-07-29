package com.fluttiris.admincontrol.qrcode.api.dto;

import com.fluttiris.admincontrol.qrcode.domain.QrCode;

import java.util.UUID;

public record QrCodeResponse(
    UUID id,
    UUID salarieId,
    String codeValeur,
    boolean actif
) {
    public static QrCodeResponse from(QrCode qrCode) {
        return new QrCodeResponse(qrCode.getId(), qrCode.getSalarieId(), qrCode.getCodeValeur(), qrCode.isActif());
    }
}
