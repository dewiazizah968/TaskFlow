package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ThemeController {

    private final UserRepository userRepository;

    @PostMapping("/theme/toggle")
    public ResponseEntity<Map<String, String>> toggleTheme(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newTheme = "night".equals(user.getThemeMode()) ? "day" : "night";

        user.setThemeMode(newTheme);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("theme", newTheme));
    }

    @PostMapping("/theme/set")
    public ResponseEntity<Map<String, String>> setTheme(
            @RequestParam String mode,
            Authentication authentication) {

        String normalized = "night".equalsIgnoreCase(mode) ? "night" : "day";

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setThemeMode(normalized);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("theme", normalized));
    }
}
