package com.taskflow.controller;

import com.taskflow.dto.DashboardResponse;
import com.taskflow.security.OAuth2LoginSuccessHandler;
import com.taskflow.service.DashboardService;
import com.taskflow.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.Authentication;

@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/register/set-password")
    public String registerSetPasswordPage(Model model, HttpSession session) {

        String email = session != null ? (String) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_GOOGLE_EMAIL) : null;
        String name = session != null ? (String) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_GOOGLE_NAME) : null;

        if (email == null) {
            return "redirect:/register";
        }

        model.addAttribute("googleEmail", email);
        model.addAttribute("googleName", name);

        return "register-set-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token, Model model) {

        boolean tokenValid = token != null && passwordResetService.validateToken(token).isPresent();

        model.addAttribute("token", token);
        model.addAttribute("tokenValid", tokenValid);

        return "reset-password";
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model, Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        DashboardResponse dashboard = dashboardService.getDashboardData(user);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);

        String[] colors = {
                "#6366f1",
                "#22c55e",
                "#f59e0b",
                "#ef4444",
                "#06b6d4",
                "#a855f7"
        };

        int colorIndex = Math.abs(user.getName().hashCode()) % colors.length;
        String avatarColor = colors[colorIndex];

        model.addAttribute("avatarColor", avatarColor);
        model.addAttribute("currentPage", "dashboard");

        return "dashboard";
    }
}
