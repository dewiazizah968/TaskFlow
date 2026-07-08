package com.taskflow.service;

import com.taskflow.entity.PasswordResetToken;
import com.taskflow.entity.User;
import com.taskflow.repository.PasswordResetTokenRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.password-reset.expiry-minutes:30}")
    private int expiryMinutes;

    @Transactional
    public void requestReset(String email) {

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/reset-password?token=" + token;

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink, expiryMinutes);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send password reset email.", ex);
        }
    }

    /**
     * Looks up a token to decide what the reset-password page should show,
     * without consuming it. Empty means the token doesn't exist, was
     * already used, or has expired.
     */
    public Optional<PasswordResetToken> validateToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isUsed())
                .filter(t -> !t.isExpired());
    }

    /**
     * Consumes the token: sets the new password and marks the token used so
     * it can never be replayed, even if the person still has the email open.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("This reset link is invalid."));

        if (resetToken.isUsed()) {
            throw new RuntimeException("This reset link has already been used.");
        }
        if (resetToken.isExpired()) {
            throw new RuntimeException("This reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
