package com.fluttiris.admincontrol.qrcode.application;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.qrcode.domain.QrCode;
import com.fluttiris.admincontrol.qrcode.domain.QrCodeRepository;
import com.fluttiris.admincontrol.salarie.domain.Salarie;
import com.fluttiris.admincontrol.salarie.domain.SalarieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final SalarieRepository salarieRepository;

    public QrCode genererPour(UUID salarieId) {
        if (qrCodeRepository.findBySalarieId(salarieId).isPresent()) {
            throw new BusinessRuleViolationException(
                "Ce salarié a déjà un QR code ; utilisez la régénération pour en émettre un nouveau");
        }
        salarieRepository.findById(salarieId)
            .orElseThrow(() -> new EntityNotFoundException("Salarié", salarieId));

        return qrCodeRepository.save(QrCode.creer(salarieId));
    }

    public QrCode regenererPour(UUID salarieId) {
        QrCode qrCode = qrCodeRepository.findBySalarieId(salarieId)
            .orElseThrow(() -> new EntityNotFoundException("QR code pour ce salarié"));
        qrCode.regenerer();
        return qrCode;
    }

    @Transactional(readOnly = true)
    public QrCode obtenirPourSalarie(UUID salarieId) {
        return qrCodeRepository.findBySalarieId(salarieId)
            .orElseThrow(() -> new EntityNotFoundException("QR code pour ce salarié"));
    }

    public QrCode desactiver(UUID salarieId) {
        QrCode qrCode = obtenirPourSalarie(salarieId);
        qrCode.desactiver();
        return qrCode;
    }

    /**
     * Simule le contrôle d'accès chantier : un scanner de badge appellerait cet
     * endpoint avec la valeur lue sur le QR code.
     */
    public record VerificationResult(boolean valide, String nom, String prenom, UUID entrepriseId) {
    }

    @Transactional(readOnly = true)
    public VerificationResult verifier(String codeValeur) {
        return qrCodeRepository.findByCodeValeur(codeValeur)
            .filter(QrCode::isActif)
            .map(qr -> {
                Salarie salarie = salarieRepository.findById(qr.getSalarieId()).orElseThrow();
                return new VerificationResult(true, salarie.getNom(), salarie.getPrenom(), salarie.getEntrepriseEmployeurId());
            })
            .orElse(new VerificationResult(false, null, null, null));
    }
}
