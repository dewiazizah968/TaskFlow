package com.taskflow.controller;

import com.taskflow.dto.TaskRequest;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskHistory;
import com.taskflow.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.taskflow.dto.StatusUpdateRequest;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public Task createTask(@RequestBody TaskRequest request) {
        return taskService.createTask(request);
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PutMapping("/{id}")
    public Task updateTask(
            @PathVariable Long id,
            @RequestBody TaskRequest request) {

        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return "Task berhasil dihapus";
    }

    @PatchMapping("/{id}/status")
    public Task updateTaskStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {

        return taskService.updateTaskStatus(id, request);
    }

    @GetMapping("/filter/status")
    public List<Task> getTasksByStatus(
            @RequestParam String status) {

        return taskService.getTasksByStatus(status);
    }

    @GetMapping("/filter/priority")
    public List<Task> getTasksByPriority(
            @RequestParam String priority) {

        return taskService.getTasksByPriority(priority);
    }

    @GetMapping("/filter/user")
    public List<Task> getTasksByAssignedUser(
            @RequestParam Long userId) {

        return taskService.getTasksByAssignedUser(userId);
    }

    @GetMapping("/{id}/history")
    public List<TaskHistory> getTaskHistory(
            @PathVariable Long id) {

        return taskService.getTaskHistory(id);
    }
}