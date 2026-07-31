package com.fluttiris.admincontrol.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final boolean smtpConfigure;

    public EmailService(JavaMailSender mailSender,
                         @Value("${app.mail-from}") String mailFrom,
                         @Value("${spring.mail.host}") String smtpHost) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.smtpConfigure = smtpHost != null && !smtpHost.isBlank();
    }

    public void envoyer(String destinataire, String sujet, String corps) {
        if (!smtpConfigure) {
            // Aucun MAIL_HOST fourni (environnement local/démo) : on journalise au
            // lieu d'échouer, pour ne pas bloquer le flux métier tant que le SMTP
            // réel n'est pas configuré côté déploiement.
            log.warn("SMTP non configuré (MAIL_HOST absent) — email non envoyé à {} : [{}] {}", destinataire, sujet, corps);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(corps);
        mailSender.send(message);
    }
}
