package com.taskflow.controller;

import com.taskflow.dto.TaskLinkRequest;
import com.taskflow.entity.TaskFile;
import com.taskflow.service.TaskFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.taskflow.entity.Task;
import com.taskflow.repository.TaskRepository;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskFileController {

    private final TaskFileService taskFileService;
    private final TaskRepository taskRepository;

    @GetMapping("/{taskId}/files")
    public List<TaskFile> getFilesByTask(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        return taskFileService.getFilesByTask(task);
    }

    @PostMapping("/{taskId}/files")
    public TaskFile uploadFile(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) throws Exception {

        return taskFileService.uploadFile(taskId, file);
    }

    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<Void> downloadFile(
            @PathVariable Long fileId) {

        return taskFileService.downloadFile(fileId);
    }

    @PostMapping("/{taskId}/links")
    public TaskFile addLink(
            @PathVariable Long taskId,
            @RequestBody TaskLinkRequest request) {

        return taskFileService.addLink(
                taskId,
                request.getTitle(),
                request.getLinkUrl()
        );
    }

    @PostMapping("/files/{fileId}/delete")
    public String deleteFile(@PathVariable Long fileId) {
        taskFileService.deleteFile(fileId);
        return "File successfully deleted";
    }
}
