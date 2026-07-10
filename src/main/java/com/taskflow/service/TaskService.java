package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.entity.*;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskHistoryRepository;
import com.taskflow.repository.TaskFileRepository;
import com.taskflow.entity.TaskFile;
import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import com.taskflow.dto.StatusUpdateRequest;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskFileRepository taskFileRepository;

    public Task createTask(TaskRequest request) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User assignedUser = userRepository.findById(request.getAssignedUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isMember = projectMemberRepository
                .findByProjectAndUser(project, assignedUser)
                .isPresent();

        if (!isMember) {
        throw new RuntimeException("User bukan anggota project ini");
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(Priority.valueOf(request.getPriority()))
                .deadline(LocalDateTime.parse(request.getDeadline()))
                .project(project)
                .assignedUser(assignedUser)
                .build();

        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
        }

        public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        }

        public Task updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User assignedUser = userRepository.findById(request.getAssignedUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isMember = projectMemberRepository
                .findByProjectAndUser(project, assignedUser)
                .isPresent();

        if (!isMember) {
                throw new RuntimeException("User bukan anggota project ini");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(Priority.valueOf(request.getPriority()));
        task.setDeadline(LocalDateTime.parse(request.getDeadline()));
        task.setProject(project);
        task.setAssignedUser(assignedUser);

        return taskRepository.save(task);
        }

        public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        taskRepository.delete(task);
        }

        public Task updateTaskStatus(Long id, StatusUpdateRequest request) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Task not found"));

        String oldStatus = task.getStatus().name();

        task.setStatus(
                TaskStatus.valueOf(request.getStatus())
        );

        Task updatedTask = taskRepository.save(task);

        TaskHistory history = TaskHistory.builder()
                .task(updatedTask)
                .action("STATUS_CHANGED")
                .oldValue(oldStatus)
                .newValue(updatedTask.getStatus().name())
                .build();

        taskHistoryRepository.save(history);

        return updatedTask;
        }

        public List<Task> getTasksByStatus(String status) {
        return taskRepository.findByStatus(
                TaskStatus.valueOf(status)
        );
        }

        public List<Task> getTasksByPriority(String priority) {
        return taskRepository.findByPriority(
                Priority.valueOf(priority)
        );
        }

        public List<Task> getTasksByAssignedUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return taskRepository.findByAssignedUser(user);
        }

        public List<TaskHistory> getTaskHistory(Long taskId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found"));

        return taskHistoryRepository.findByTask(task);
        }

        public Task updateTaskStatusByUser(
                Long taskId,
                StatusUpdateRequest request,
                User currentUser) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        boolean isOwner = task.getProject()
                .getOwner()
                .getId()
                .equals(currentUser.getId());

        boolean isAssignedUser = task.getAssignedUser()
                .getId()
                .equals(currentUser.getId());

        if (!isOwner && !isAssignedUser) {
                throw new RuntimeException("You do not have access to change the status of this task.");
        }

        String oldStatus = task.getStatus().name();

        task.setStatus(TaskStatus.valueOf(request.getStatus()));

        Task updatedTask = taskRepository.save(task);

        TaskHistory history = TaskHistory.builder()
                .task(updatedTask)
                .action("STATUS_CHANGED")
                .oldValue(oldStatus)
                .newValue(updatedTask.getStatus().name())
                .build();

        taskHistoryRepository.save(history);

        return updatedTask;
        }

    /** Delete a task + all its files (owner of project only) */
    public void deleteTaskFull(Long taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getProject().getOwner().getId().equals(user.getId()))
            throw new RuntimeException("Only the project owner can delete tasks.");
        for (TaskFile file : taskFileRepository.findByTask(task)) {
            if (file.getFilePath() != null) {
                try { Files.deleteIfExists(Paths.get(file.getFilePath())); } catch (Exception ignored) {}
            }
            taskFileRepository.delete(file);
        }
        taskHistoryRepository.deleteAll(taskHistoryRepository.findByTask(task));
        taskRepository.delete(task);
    }

    /** Edit task fields (owner of project only) */
    public Task editTask(Long taskId, TaskRequest request, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getProject().getOwner().getId().equals(user.getId()))
            throw new RuntimeException("Only the project owner can edit tasks.");
        if (request.getTitle() != null) task.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) task.setDescription(request.getDescription().trim());
        if (request.getPriority() != null) task.setPriority(Priority.valueOf(request.getPriority()));
        if (request.getDeadline() != null && !request.getDeadline().isBlank()) {
            task.setDeadline(LocalDateTime.parse(request.getDeadline()));
        }
        if (request.getAssignedUserId() != null) {
            User assignee = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            task.setAssignedUser(assignee);
        }
        return taskRepository.save(task);
    }

}