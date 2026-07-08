package com.taskflow.controller;

import com.taskflow.entity.Notification;
import com.taskflow.repository.NotificationRepository;
import com.taskflow.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.Authentication;

@Controller
@RequiredArgsConstructor
public class NotificationPageController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping("/notifications")
    public String notificationPage(
            Model model,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User tidak ditemukan"));

        model.addAttribute(
                "notifications",
                notificationRepository.findByUser(user)
        );

        model.addAttribute("user", user);

        return "notifications";
    }
}