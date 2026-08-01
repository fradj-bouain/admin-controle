package com.fluttiris.admincontrol.qrcode.api;

import com.fluttiris.admincontrol.qrcode.api.dto.QrCodeResponse;
import com.fluttiris.admincontrol.qrcode.api.dto.VerificationResponse;
import com.fluttiris.admincontrol.qrcode.api.dto.VerifierQrCodeRequest;
import com.fluttiris.admincontrol.qrcode.application.QrCodeService;
import com.fluttiris.admincontrol.qrcode.application.QrImageGenerator;
import com.fluttiris.admincontrol.qrcode.domain.QrCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final QrImageGenerator qrImageGenerator;

    @PostMapping("/api/v1/salaries/{salarieId}/qrcode")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<QrCodeResponse> generer(@PathVariable UUID salarieId) {
        QrCode qrCode = qrCodeService.genererPour(salarieId);
        return ResponseEntity.status(HttpStatus.CREATED).body(QrCodeResponse.from(qrCode));
    }

    @PostMapping("/api/v1/salaries/{salarieId}/qrcode/regenerer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public QrCodeResponse regenerer(@PathVariable UUID salarieId) {
        return QrCodeResponse.from(qrCodeService.regenererPour(salarieId));
    }

    @GetMapping("/api/v1/salaries/{salarieId}/qrcode")
    @PreAuthorize("isAuthenticated()")
    public QrCodeResponse obtenir(@PathVariable UUID salarieId) {
        return QrCodeResponse.from(qrCodeService.obtenirPourSalarie(salarieId));
    }

    @PostMapping("/api/v1/salaries/{salarieId}/qrcode/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public QrCodeResponse desactiver(@PathVariable UUID salarieId) {
        return QrCodeResponse.from(qrCodeService.desactiver(salarieId));
    }

    /** Image PNG du QR code, prête à être affichée/imprimée sur la carte d'identité numérique. */
    @GetMapping(value = "/api/v1/salaries/{salarieId}/qrcode/image", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> image(@PathVariable UUID salarieId) {
        QrCode qrCode = qrCodeService.obtenirPourSalarie(salarieId);
        byte[] png = qrImageGenerator.genererPng(qrCode.getCodeValeur());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    /** Simule la lecture d'un badge par un contrôleur à l'entrée du chantier. */
    @PostMapping("/api/v1/qrcodes/verifier")
    @PreAuthorize("isAuthenticated()")
    public VerificationResponse verifier(@Valid @RequestBody VerifierQrCodeRequest request) {
        return VerificationResponse.from(qrCodeService.verifier(request.codeValeur()));
    }
}
