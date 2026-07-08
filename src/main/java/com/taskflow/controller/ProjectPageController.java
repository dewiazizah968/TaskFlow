package com.taskflow.controller;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.TaskFileRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectMemberService;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskFileService;
import com.taskflow.service.TaskService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.taskflow.entity.Project;
import com.taskflow.entity.TaskFile;
import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.StatusUpdateRequest;
import org.springframework.web.multipart.MultipartFile;
import com.taskflow.dto.AddMemberRequest;
import com.taskflow.entity.User;
import com.taskflow.entity.TaskStatus;

import java.util.HashMap;

import org.springframework.security.core.Authentication;

import com.taskflow.entity.Task;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProjectPageController {

    private final ProjectService projectService;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskService taskService;
    private final TaskFileService taskFileService;
    private final ProjectMemberService projectMemberService;
    private final UserRepository userRepository;
    private final TaskFileRepository taskFileRepository;

    @GetMapping("/projects")
    public String projectPage(Model model, Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        List<Project> projects = projectService.getProjectsForUser(user);

        Map<Long, Double> projectProgressMap = new HashMap<>();
        Map<Long, String> projectStatusMap = new HashMap<>();

        for (Project project : projects) {
            long totalTasks = taskRepository.countByProject(project);
            long completedTasks = taskRepository.countByProjectAndStatus(project, TaskStatus.DONE);
            long inProgressTasks = taskRepository.countByProjectAndStatus(project, TaskStatus.IN_PROGRESS);

            double progress = 0;

            if (totalTasks > 0) {
                progress = ((double) completedTasks / totalTasks) * 100;
            }

            String status;

            if (totalTasks == 0) {
                status = "NOT YET";
            } else if (completedTasks == totalTasks) {
                status = "DONE";
            } else if (inProgressTasks > 0) {
                status = "IN PROGRESS";
            } else {
                status = "NOT YET";
            }

            projectProgressMap.put(project.getId(), progress);
            projectStatusMap.put(project.getId(), status);
        }

        model.addAttribute("projects", projects);
        model.addAttribute("projectProgressMap", projectProgressMap);
        model.addAttribute("projectStatusMap", projectStatusMap);
        model.addAttribute("projectRequest", new ProjectRequest());
        model.addAttribute("user", user);

        String[] colors = {
                "#6366f1",
                "#22c55e",
                "#f59e0b",
                "#ef4444",
                "#06b6d4",
                "#a855f7"
        };

        int colorIndex =
                Math.abs(user.getName().hashCode()) % colors.length;

        String avatarColor = colors[colorIndex];

        model.addAttribute("avatarColor", avatarColor);
        model.addAttribute("currentPage", "projects");

        return "projects";
    }

    @PostMapping("/projects/create")
    public String createProject(
            @ModelAttribute ProjectRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User tidak ditemukan"));

        projectService.createProject(request, user);

        return "redirect:/projects";
    }

    @GetMapping("/projects/{id}")
    public String projectDetail(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        String[] colors = {
                "#6366f1", "#22c55e", "#f59e0b",
                "#ef4444", "#06b6d4", "#a855f7"
        };
        String avatarColor = colors[Math.abs(user.getName().hashCode()) % colors.length];

        model.addAttribute("user", user);
        model.addAttribute("avatarColor", avatarColor);

        Project project = projectService.getProjectById(id);

        List<Task> tasks = taskRepository.findByProject(project);

        long totalTasks = taskRepository.countByProject(project);
        long completedTasks = taskRepository.countByProjectAndStatus(project, TaskStatus.DONE);
        long inProgressTasks = taskRepository.countByProjectAndStatus(project, TaskStatus.IN_PROGRESS);

        double projectProgress = 0;

        if (totalTasks > 0) {
            projectProgress = ((double) completedTasks / totalTasks) * 100;
        }

        String projectStatus;

        if (totalTasks == 0) {
            projectStatus = "NOT YET";
        } else if (completedTasks == totalTasks) {
            projectStatus = "DONE";
        } else if (inProgressTasks > 0) {
            projectStatus = "IN PROGRESS";
        } else {
            projectStatus = "NOT YET";
        }

        Map<Long, List<TaskFile>> taskFilesMap = new HashMap<>();

        for (Task task : tasks) {
            taskFilesMap.put(
                    task.getId(),
                    taskFileRepository.findByTask(task)
            );
        }

        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("taskFilesMap", taskFilesMap);
        model.addAttribute("taskRequest", new TaskRequest());
        model.addAttribute("members", projectMemberRepository.findByProject(project));

        model.addAttribute("projectProgress", projectProgress);
        model.addAttribute("projectStatus", projectStatus);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("currentPage", "projects");

        return "project-detail";
    }

    @PostMapping("/projects/{id}/tasks/create")
    public String createTaskFromProjectPage(
            @PathVariable Long id,
            @ModelAttribute TaskRequest request) {

        request.setProjectId(id);
        taskService.createTask(request);

        return "redirect:/projects/" + id;
    }

    @PostMapping("/projects/{projectId}/tasks/{taskId}/status")
    public String updateTaskStatusFromUI(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam String status,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(status);

        try {
            taskService.updateTaskStatusByUser(taskId, request, user);
        } catch (RuntimeException ex) {
            return "redirect:/projects/" + projectId + "?statusError=" +
                    java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/projects/{projectId}/tasks/{taskId}/files/upload")
    public String uploadTaskFileFromUI(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) throws Exception {

        taskFileService.uploadFile(taskId, file);

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/projects/{projectId}/members/add")
    public String addMemberFromUI(
            @PathVariable Long projectId,
            @RequestParam String email) {

        AddMemberRequest request = new AddMemberRequest();
        request.setEmail(email);

        try {
            projectMemberService.addMember(projectId, request);
        } catch (RuntimeException ex) {
            return "redirect:/projects/" + projectId + "?error=" +
                java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/projects/{projectId}/files/{fileId}/delete")
    public String deleteAttachmentFromUI(
            @PathVariable Long projectId,
            @PathVariable Long fileId) {

        taskFileService.deleteFile(fileId);

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/projects/{projectId}/tasks/{taskId}/links/add")
    public String addLinkFromUI(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam String title,
            @RequestParam String linkUrl) {

        taskFileService.addLink(taskId, title, linkUrl);

        return "redirect:/projects/" + projectId;
    }

    /** DELETE PROJECT (owner only) */
    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        try {
            projectService.deleteProjectFull(id, user);
        } catch (RuntimeException ex) {
            return "redirect:/projects?error=" +
                java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/projects";
    }

    /** EDIT PROJECT (owner only) */
    @PostMapping("/projects/{id}/edit")
    public String editProject(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam String description,
                              Authentication authentication) {
        User user = getUser(authentication);
        try {
            projectService.editProject(id, name, description, user);
        } catch (RuntimeException ex) {
            return "redirect:/projects/" + id + "?error=" +
                java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/projects/" + id;
    }

    /** REMOVE MEMBER (owner only) */
    @PostMapping("/projects/{projectId}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long projectId,
                               @PathVariable Long memberId,
                               Authentication authentication) {
        User user = getUser(authentication);
        try {
            projectService.removeMember(projectId, memberId, user);
        } catch (RuntimeException ex) {
            return "redirect:/projects/" + projectId + "?error=" +
                java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/projects/" + projectId;
    }

    /** DELETE TASK (owner only) */
    @PostMapping("/projects/{projectId}/tasks/{taskId}/delete")
    public String deleteTask(@PathVariable Long projectId,
                             @PathVariable Long taskId,
                             Authentication authentication) {
        User user = getUser(authentication);
        try {
            taskService.deleteTaskFull(taskId, user);
        } catch (RuntimeException ex) {
            return "redirect:/projects/" + projectId + "?statusError=" +
                java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/projects/" + projectId;
    }

    /** EDIT TASK (owner only) */
    @PostMapping("/projects/{projectId}/tasks/{taskId}/edit")
    public String editTask(@PathVariable Long projectId,
                           @PathVariable Long taskId,
                           @ModelAttribute TaskRequest request,
                           Authentication authentication) {
        User user = getUser(authentication);
        try {
            taskService.editTask(taskId, request, user);
        } catch (RuntimeException ex) {
            return "redirect:/projects/" + projectId + "?statusError=" +
                java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/projects/" + projectId;
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
    }

}