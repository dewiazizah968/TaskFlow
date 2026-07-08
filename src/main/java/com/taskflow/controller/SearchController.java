package com.taskflow.controller;

import com.taskflow.entity.Project;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import com.taskflow.dto.SearchResponse;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    /** Legacy page-based search — kept for backward compatibility */
    @GetMapping("/search")
    public String search(
            @RequestParam String keyword,
            Authentication authentication,
            Model model) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Project> userProjects = projectService.getProjectsForUser(user);

        List<Project> projectResults = projectRepository
                .findByNameContainingIgnoreCase(keyword)
                .stream().filter(userProjects::contains).toList();

        List<Task> taskResults = taskRepository
                .findByTitleContainingIgnoreCase(keyword)
                .stream().filter(t -> userProjects.contains(t.getProject())).toList();

        model.addAttribute("keyword", keyword);
        model.addAttribute("projectResults", projectResults);
        model.addAttribute("taskResults", taskResults);
        return "search-results";
    }

    /** AJAX endpoint used by the search popup on every page */
    @GetMapping("/api/search")
    @ResponseBody
    public SearchResponse apiSearch(
            @RequestParam String keyword,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Project> userProjects = projectService.getProjectsForUser(user);

        List<SearchResponse.ProjectItem> projects = projectRepository
                .findByNameContainingIgnoreCase(keyword)
                .stream()
                .filter(userProjects::contains)
                .map(p -> new SearchResponse.ProjectItem(p.getId(), p.getName(), p.getDescription()))
                .toList();

        List<SearchResponse.TaskItem> tasks = taskRepository
                .findByTitleContainingIgnoreCase(keyword)
                .stream()
                .filter(t -> userProjects.contains(t.getProject()))
                .map(t -> new SearchResponse.TaskItem(
                        t.getId(), t.getTitle(), t.getDescription(),
                        t.getStatus() != null ? t.getStatus().name() : "",
                        t.getProject().getId(), t.getProject().getName()))
                .toList();

        return new SearchResponse(keyword, projects, tasks);
    }
}