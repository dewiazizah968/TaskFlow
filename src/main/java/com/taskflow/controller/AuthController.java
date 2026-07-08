package com.taskflow.controller;

import com.taskflow.dto.ForgotPasswordRequest;
import com.taskflow.dto.GoogleSetPasswordRequest;
import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.dto.ResetPasswordRequest;
import com.taskflow.security.OAuth2LoginSuccessHandler;
import com.taskflow.service.PasswordResetService;
import com.taskflow.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    // Accepts standard email formats: name@gmail.com, name@email.com,
    // name@company.co.id, or any other valid domain.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {

        Map<String, String> error = new HashMap<>();

        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            error.put("message", "Please enter a valid email address.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            error.put("message", "Password must be at least 6 characters.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            userService.register(request);
        } catch (RuntimeException ex) {
            String message = "Email sudah terdaftar".equals(ex.getMessage())
                    ? "This email is already registered."
                    : "Registration failed. Please try again.";
            error.put("message", message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        return ResponseEntity.ok(Map.of("message", "Registration successful."));
    }

    @PostMapping("/login")
    @ResponseBody
    public String login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/forgot-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {

        Map<String, String> body = new HashMap<>();

        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            body.put("message", "Please enter a valid email address.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        passwordResetService.requestReset(request.getEmail().trim().toLowerCase());

        body.put("message", "If that email is registered, we've sent a password reset link to it.");
        return ResponseEntity.ok(body);
    }

    /**
     * Completes a password reset using the token from the emailed link.
     */
    @PostMapping("/reset-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {

        Map<String, String> body = new HashMap<>();

        if (request.getToken() == null || request.getToken().isBlank()) {
            body.put("message", "Missing or invalid reset link.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            body.put("message", "Password must be at least 6 characters.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            body.put("message", "Passwords do not match.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        } catch (RuntimeException ex) {
            body.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        body.put("message", "Your password has been reset. You can now log in.");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/register-google")
    @ResponseBody
    public ResponseEntity<Map<String, String>> registerGoogle(@RequestBody GoogleSetPasswordRequest request,
                                                                HttpServletRequest httpRequest) {

        Map<String, String> error = new HashMap<>();
        HttpSession session = httpRequest.getSession(false);

        String email = session != null ? (String) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_GOOGLE_EMAIL) : null;
        String name = session != null ? (String) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_GOOGLE_NAME) : null;
        String googleId = session != null ? (String) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_GOOGLE_ID) : null;

        if (email == null || googleId == null) {
            error.put("message", "Your Google verification has expired. Please try again.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            error.put("message", "Password must be at least 6 characters.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            error.put("message", "Passwords do not match.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            userService.registerFromGoogle(email, name, googleId, request.getPassword());
        } catch (RuntimeException ex) {
            String message = "Email sudah terdaftar".equals(ex.getMessage())
                    ? "This email is already registered."
                    : "Registration failed. Please try again.";
            error.put("message", message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        session.removeAttribute(OAuth2LoginSuccessHandler.SESSION_GOOGLE_EMAIL);
        session.removeAttribute(OAuth2LoginSuccessHandler.SESSION_GOOGLE_NAME);
        session.removeAttribute(OAuth2LoginSuccessHandler.SESSION_GOOGLE_ID);

        return ResponseEntity.ok(Map.of("message", "Registration successful."));
    }
}
