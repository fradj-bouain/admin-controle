package com.fluttiris.admincontrol.qrcode.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {

    Optional<QrCode> findBySalarieId(UUID salarieId);

    Optional<QrCode> findByCodeValeur(String codeValeur);
}
