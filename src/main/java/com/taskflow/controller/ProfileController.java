package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /* ─ Upload photo ─ */
    @PostMapping("/profile/upload")
    public String uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {

        User user = getUser(authentication);
        profileService.uploadProfileImage(user, file);
        return "redirect:/dashboard";
    }

    /* ─ Serve photo ─ */
    @GetMapping("/profile/image")
    @ResponseBody
    public ResponseEntity<Resource> getProfileImage(Authentication authentication) throws Exception {
        User user = getUser(authentication);
        if (user.getProfileImagePath() == null) return ResponseEntity.notFound().build();
        Path path = Paths.get(user.getProfileImagePath());
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);
    }

    /* ─ Update name & email ─ */
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

    /* ─ Change password ─ */
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
    @ResponseBody
    public ResponseEntity<Resource> getUserImage(@PathVariable Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        if (user.getProfileImagePath() == null) return ResponseEntity.notFound().build();
        Path path = Paths.get(user.getProfileImagePath());
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
    }
}
