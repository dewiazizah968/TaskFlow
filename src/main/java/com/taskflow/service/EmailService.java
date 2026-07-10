package com.taskflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Sends emails via Brevo's HTTPS Transactional Email API instead of SMTP.
 *
 * Some hosts (e.g. Railway's free/trial/hobby plans) block outbound SMTP
 * ports (25/465/587) entirely to prevent abuse, which breaks
 * JavaMailSender-based email sending. The HTTPS API achieves the same
 * result (Brevo delivers the email) without needing an SMTP connection
 * at all, so it works on any host.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${app.mail.from-address:no-reply@taskflow.local}")
    private String fromAddress;

    @Value("${app.mail.sender-name:TaskFlow}")
    private String senderName;

    public void sendPasswordResetEmail(String toEmail, String resetLink, int expiryMinutes) throws Exception {

        String textContent =
                "We received a request to reset your TaskFlow password.\n\n" +
                "Click the link below to choose a new password. This link is valid for " +
                expiryMinutes + " minutes and can only be used once:\n\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email - your " +
                "password will remain unchanged.";

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", fromAddress),
                "to", java.util.List.of(Map.of("email", toEmail)),
                "subject", "Reset your TaskFlow password",
                "textContent", textContent
        );

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_API_URL))
                .header("accept", "application/json")
                .header("api-key", brevoApiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Brevo API returned status " + response.statusCode() + ": " + response.body()
            );
        }
    }
}
