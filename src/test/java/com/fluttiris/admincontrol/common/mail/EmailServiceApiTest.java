package com.fluttiris.admincontrol.common.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailServiceApiTest {

    private final JavaMailSender smtp = mock(JavaMailSender.class);
    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile int statusReponse = 200;

    @BeforeEach
    void demarrerFauxServeur() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] reponse = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusReponse, reponse.length);
            exchange.getResponseBody().write(reponse);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void arreterFauxServeur() {
        server.stop(0);
    }

    private EmailService service(String token) {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/send/12345";
        return new EmailService(smtp, "no-reply@admincontrol-btp.fr", "", url, token);
    }

    @Test
    void envoieParApiHttpsAuFormatMailtrapSansPasserParSmtp() throws IOException {
        service("secret-token").envoyer("client@exemple.test", "Sujet é", "Corps du message");

        assertThat(path.get()).isEqualTo("/api/send/12345");
        assertThat(authorization.get()).isEqualTo("Bearer secret-token");
        JsonNode json = new ObjectMapper().readTree(body.get());
        assertThat(json.at("/from/email").asText()).isEqualTo("no-reply@admincontrol-btp.fr");
        assertThat(json.at("/to/0/email").asText()).isEqualTo("client@exemple.test");
        assertThat(json.get("subject").asText()).isEqualTo("Sujet é");
        assertThat(json.get("text").asText()).isEqualTo("Corps du message");
        verifyNoInteractions(smtp);
    }

    @Test
    void uneReponseEnErreurRemonteUneExceptionAuLieuDeFairePasserPourEnvoye() {
        statusReponse = 401;

        assertThatThrownBy(() -> service("mauvais-token").envoyer("client@exemple.test", "Sujet", "Corps"))
            .isInstanceOf(MailSendException.class)
            .hasMessageContaining("HTTP 401");
    }

    @Test
    void sansTokenLApiNEstPasUtilisee() {
        service("").envoyer("client@exemple.test", "Sujet", "Corps");

        assertThat(path.get()).isNull();
    }
}
