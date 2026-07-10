package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.entity.Project;
import com.taskflow.entity.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.taskflow.dto.ProjectDashboardResponse;
import com.taskflow.entity.TaskStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.entity.ProjectMember;
import com.taskflow.repository.TaskRepository;
import com.taskflow.entity.Task;
import com.taskflow.repository.TaskFileRepository;
import com.taskflow.repository.TaskHistoryRepository;
import com.taskflow.entity.TaskFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.taskflow.repository.ProjectMemberRepository;
import java.util.ArrayList;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskFileRepository taskFileRepository;
    private final TaskHistoryRepository taskHistoryRepository;

    public Project createProject(ProjectRequest request) {

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        return projectRepository.save(project);
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public Project updateProject(Long id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        projectRepository.delete(project);
    }

    public ProjectDashboardResponse getProjectDashboard(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        long totalTasks = taskRepository.countByProject(project);
        long completedTasks = taskRepository.countByProjectAndStatus(project, TaskStatus.DONE);

        double progressPercentage = 0;

        if (totalTasks > 0) {
            progressPercentage = ((double) completedTasks / totalTasks) * 100;
        }

        return ProjectDashboardResponse.builder()
                .projectName(project.getName())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .progressPercentage(progressPercentage)
                .build();
    }

    public List<Project> getProjectsForUser(User user) {

        List<Project> projects = new ArrayList<>();

        // projects created/owned by users
        projects.addAll(projectRepository.findByOwner(user));

        // project in which the user becomes a member
        List<ProjectMember> memberships = projectMemberRepository.findByUser(user);

        for (ProjectMember member : memberships) {
            Project project = member.getProject();

            if (!projects.contains(project)) {
                projects.add(project);
            }
        }

        return projects;
    }

    public Project createProject(ProjectRequest request, User owner) {

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        Project saved = projectRepository.save(project);

        // Auto-add owner as member so solo projects work out of the box
        ProjectMember ownerMember = ProjectMember.builder()
                .project(saved)
                .user(owner)
                .build();
        projectMemberRepository.save(ownerMember);

        return saved;
    }

    /** Delete project + all tasks, files, members (owner only) */
    public void deleteProjectFull(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwner().getId().equals(user.getId()))
            throw new RuntimeException("Only the project owner can delete this project.");

        List<Task> tasks = taskRepository.findByProject(project);
        for (Task task : tasks) {
            // Delete physical files
            for (TaskFile file : taskFileRepository.findByTask(task)) {
                if (file.getFilePath() != null) {
                    try { Files.deleteIfExists(Paths.get(file.getFilePath())); } catch (Exception ignored) {}
                }
                taskFileRepository.delete(file);
            }
            taskHistoryRepository.deleteAll(taskHistoryRepository.findByTask(task));
            taskRepository.delete(task);
        }
        projectMemberRepository.deleteAll(projectMemberRepository.findByProject(project));
        projectRepository.delete(project);
    }

    /** Edit project name & description (owner only) */
    public Project editProject(Long projectId, String name, String description, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwner().getId().equals(user.getId()))
            throw new RuntimeException("Only the project owner can edit this project.");
        project.setName(name.trim());
        project.setDescription(description.trim());
        return projectRepository.save(project);
    }

    /** Remove a member from the project (owner only, cannot remove owner) */
    public void removeMember(Long projectId, Long memberId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwner().getId().equals(user.getId()))
            throw new RuntimeException("Only the project owner can remove members.");
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (member.getUser().getId().equals(project.getOwner().getId()))
            throw new RuntimeException("Cannot remove the project owner.");
        projectMemberRepository.delete(member);
    }

}