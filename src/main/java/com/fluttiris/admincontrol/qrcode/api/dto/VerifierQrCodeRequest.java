package com.fluttiris.admincontrol.qrcode.api.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifierQrCodeRequest(@NotBlank String codeValeur) {
}
