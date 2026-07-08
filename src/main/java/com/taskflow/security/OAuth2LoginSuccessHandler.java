package com.taskflow.security;

import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Runs right after Google confirms who the user is (Spring Security's
 * oauth2Login flow already guarantees the Google account genuinely exists
 * and the email is real, since Google itself performed that check).
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public static final String SESSION_GOOGLE_EMAIL = "pendingGoogleEmail";
    public static final String SESSION_GOOGLE_NAME = "pendingGoogleName";
    public static final String SESSION_GOOGLE_ID = "pendingGoogleId";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("sub");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        if (email == null || Boolean.FALSE.equals(emailVerified)) {
            SecurityContextHolder.clearContext();
            response.sendRedirect("/login?error=google_unverified");
            return;
        }

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                userRepository.save(user);
            }
            // Authentication is already set by Spring Security's OAuth2 filter,
            // and the "google" registration is configured with
            // user-name-attribute=email, so authentication.getName() resolves
            // to the same email the rest of the app already relies on.
            response.sendRedirect("/dashboard");
            return;
        }

        // No account yet: clear the OAuth2 session authentication (so the
        // person can't browse the app before finishing registration) and
        // stash the verified Google profile for the set-password step.
        request.getSession().invalidate();
        HttpSession freshSession = request.getSession(true);
        freshSession.setAttribute(SESSION_GOOGLE_EMAIL, email);
        freshSession.setAttribute(SESSION_GOOGLE_NAME, name != null ? name : email);
        freshSession.setAttribute(SESSION_GOOGLE_ID, googleId);

        SecurityContextHolder.clearContext();
        response.sendRedirect("/register/set-password");
    }
}
