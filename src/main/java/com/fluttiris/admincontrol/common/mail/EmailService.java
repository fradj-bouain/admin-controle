package com.fluttiris.admincontrol.common.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final boolean smtpConfigure;
    private final String apiUrl;
    private final String apiToken;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailService(JavaMailSender mailSender,
                         @Value("${app.mail-from}") String mailFrom,
                         @Value("${spring.mail.host}") String smtpHost,
                         @Value("${app.mail-api.url:}") String apiUrl,
                         @Value("${app.mail-api.token:}") String apiToken) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.smtpConfigure = smtpHost != null && !smtpHost.isBlank();
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
    }

    public void envoyer(String destinataire, String sujet, String corps) {
        // SMTP sortant bloqué chez certains hébergeurs (ex. Railway hors plan Pro) : API HTTPS Mailtrap à la place.
        if (apiUrl != null && !apiUrl.isBlank() && apiToken != null && !apiToken.isBlank()) {
            envoyerParApi(destinataire, sujet, corps);
            return;
        }
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

    private void envoyerParApi(String destinataire, String sujet, String corps) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                "from", Map.of("email", mailFrom),
                "to", List.of(Map.of("email", destinataire)),
                "subject", sujet,
                "text", corps));
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new MailSendException("API email : HTTP " + response.statusCode() + " — " + response.body());
            }
        } catch (JsonProcessingException e) {
            throw new MailSendException("API email : corps de requête invalide", e);
        } catch (IOException e) {
            throw new MailSendException("API email injoignable : " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MailSendException("API email : envoi interrompu", e);
        }
    }
}
