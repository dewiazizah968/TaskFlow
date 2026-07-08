package com.taskflow.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address:${spring.mail.username:no-reply@taskflow.local}}")
    private String fromAddress;

    @Value("${app.mail.sender-name:TaskFlow}")
    private String senderName;

    public void sendPasswordResetEmail(String toEmail, String resetLink, int expiryMinutes) throws Exception {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toEmail);
        helper.setSubject("Reset your TaskFlow password");
        helper.setText(
                "We received a request to reset your TaskFlow password.\n\n" +
                "Click the link below to choose a new password. This link is valid for " +
                expiryMinutes + " minutes and can only be used once:\n\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email - your " +
                "password will remain unchanged.",
                false
        );

        mailSender.send(mimeMessage);
    }
}