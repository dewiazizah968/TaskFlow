package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /* - Upload photo - */
    @PostMapping("/profile/upload")
    public String uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {

        User user = getUser(authentication);
        profileService.uploadProfileImage(user, file);
        return "redirect:/dashboard";
    }

    /* - Serve photo: now just a redirect to the Cloudinary URL - */
    @GetMapping("/profile/image")
    public ResponseEntity<Void> getProfileImage(Authentication authentication) {
        User user = getUser(authentication);
        return redirectToImage(user);
    }

    /* - Update name & email - */
    @PostMapping("/profile/update")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateProfile(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            Authentication authentication) {

        User user = getUser(authentication);
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated."));
    }

    /* - Change password - */
    @PostMapping("/profile/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            Authentication authentication) {

        User user = getUser(authentication);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Current password is incorrect."));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "New password must be at least 6 characters."));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    /** Serve any user's profile image by their ID (for member lists) */
    @GetMapping("/profile/image/{userId}")
    public ResponseEntity<Void> getUserImage(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        return redirectToImage(user);
    }

    private ResponseEntity<Void> redirectToImage(User user) {
        if (user.getProfileImagePath() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(user.getProfileImagePath()))
                .build();
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
    }
}
