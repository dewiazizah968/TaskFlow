package com.taskflow.controller;

import com.taskflow.dto.CalendarEventResponse;
import com.taskflow.entity.Project;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import java.util.List;
import org.springframework.ui.Model;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectService projectService;

    @GetMapping("/calendar")
    public String calendarPage(
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

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

        model.addAttribute("user", user);
        model.addAttribute("avatarColor", avatarColor);
        model.addAttribute("currentPage", "calendar");

        return "calendar";
    }

    @GetMapping("/api/calendar/events")
    @ResponseBody
    public List<CalendarEventResponse> getCalendarEvents(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        List<Project> userProjects = projectService.getProjectsForUser(user);

        return taskRepository.findAll()
                .stream()
                .filter(task -> userProjects.contains(task.getProject()))
                .filter(task -> task.getDeadline() != null)
                .map(task -> {

                    String color;

                    boolean isOverdue =
                            task.getDeadline().isBefore(LocalDateTime.now())
                            && task.getStatus() != TaskStatus.DONE;

                    if (isOverdue) {
                        color = "#ef4444";
                    } else {
                        switch (task.getStatus()) {
                            case DONE:
                                color = "#22c55e";
                                break;

                            case IN_PROGRESS:
                                color = "#f59e0b";
                                break;

                            default:
                                color = "#6b7280";
                        }
                    }

                    return CalendarEventResponse.builder()
                            .taskId(task.getId())
                            .title(isOverdue ? "[OVERDUE] " + task.getTitle() : task.getTitle())
                            .start(task.getDeadline().toLocalDate().toString())
                            .allDay(true)
                            .color(color)
                            .projectId(task.getProject().getId())
                            .projectName(task.getProject().getName())
                            .status(task.getStatus().name())
                            .priority(task.getPriority().name())
                            .assignedUser(task.getAssignedUser() != null
                                    ? task.getAssignedUser().getName()
                                    : "-")
                            .deadline(task.getDeadline().toString())
                            .build();
                })
                .toList();
    }
}