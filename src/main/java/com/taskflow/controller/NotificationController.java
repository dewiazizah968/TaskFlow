package com.taskflow.controller;

import com.taskflow.entity.Notification;
import com.taskflow.entity.User;
import com.taskflow.repository.NotificationRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.taskflow.dto.NotificationResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @PostMapping("/generate-deadline")
    public String generateDeadlineNotifications() {
        notificationService.generateDeadlineNotifications();
        return "Notifikasi deadline berhasil dibuat";
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getNotificationsByUser(@PathVariable Long userId) {
        return notificationService.getNotificationsByUser(userId);
    }

    /** Returns unread notifications for the logged-in user — used by dashboard popup */
    @GetMapping("/me")
    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.findByUserAndIsReadFalse(user)
                .stream()
                .map(n -> {
                    // Project comes either directly (invite notifications) or via the task (deadline notifications)
                    var project = n.getProject() != null
                            ? n.getProject()
                            : (n.getTask() != null ? n.getTask().getProject() : null);

                    return NotificationResponse.builder()
                            .id(n.getId())
                            .message(n.getMessage())
                            .isRead(n.isRead())
                            .type(n.getType() != null ? n.getType() : "TASK_DEADLINE")
                            .taskId(n.getTask() != null ? n.getTask().getId() : null)
                            .taskTitle(n.getTask() != null ? n.getTask().getTitle() : null)
                            .projectId(project != null ? project.getId() : null)
                            .projectName(project != null ? project.getName() : null)
                            .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : "")
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** Mark a single notification as read; returns updated unread count */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Long>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });

        long unread = notificationRepository.countByUserAndIsReadFalse(user);
        return ResponseEntity.ok(Map.of("unreadCount", unread));
    }
}