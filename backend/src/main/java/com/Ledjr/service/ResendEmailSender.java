package com.Ledjr.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * really sends the email, through Resend (resend.com). it's one HTTP call:
 * POST https://api.resend.com/emails with the api key, and {from, to, subject, text} as JSON.
 *
 * never throws. if Resend is down we log it and move on, because the forgot password endpoint has to
 * answer the same way whether the email has an account or not. an error only for real accounts would
 * give that away
 */
public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
    private static final URI SEND_URL = URI.create("https://api.resend.com/emails");

    private final String apiKey;
    private final String from;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    // builds the JSON so quotes or newlines in the text can't break it
    private final ObjectMapper json = new ObjectMapper();

    public ResendEmailSender(String apiKey, String from){
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String text){
        try {
            String body = json.writeValueAsString(Map.of(
                "from", from,
                "to", List.of(to),
                "subject", subject,
                "text", text));

            HttpRequest request = HttpRequest.newBuilder(SEND_URL)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                // Resend's error says what's wrong (bad key, domain not verified...). never has the link in it
                log.error("Resend didn't send the email ({}): {}", response.statusCode(), response.body());
            }
        } catch (IOException e) {
            log.error("Couldn't reach Resend to send an email", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while sending an email");
        }
    }
}
