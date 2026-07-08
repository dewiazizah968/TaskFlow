package com.taskflow.service;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskFile;
import com.taskflow.repository.TaskFileRepository;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskFileService {

    private final TaskRepository taskRepository;
    private final TaskFileRepository taskFileRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public List<TaskFile> getFilesByTask(Task task) {
        return taskFileRepository.findByTask(task);
    }

    public TaskFile uploadFile(Long taskId, MultipartFile file) throws Exception {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));

        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        Path filePath = uploadPath.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        TaskFile taskFile = TaskFile.builder()
                .fileName(originalFileName)
                .fileType(file.getContentType())
                .filePath(filePath.toString())
                .attachmentType("FILE")
                .task(task)
                .build();

        return taskFileRepository.save(taskFile);
    }

    public TaskFile addLink(Long taskId, String title, String linkUrl) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));

        TaskFile taskFile = TaskFile.builder()
                .fileName(title)
                .fileType("LINK")
                .linkUrl(linkUrl)
                .attachmentType("LINK")
                .task(task)
                .build();

        return taskFileRepository.save(taskFile);
    }

    public ResponseEntity<Resource> downloadFile(Long fileId) throws MalformedURLException {
        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File tidak ditemukan"));

        Path path = Paths.get(taskFile.getFilePath());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File tidak ditemukan di folder uploads");
        }

        String contentType = taskFile.getFileType();

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        ContentDisposition disposition;

        if (contentType.startsWith("image")) {
            disposition = ContentDisposition.inline()
                    .filename(taskFile.getFileName())
                    .build();
        } else {
            disposition = ContentDisposition.attachment()
                    .filename(taskFile.getFileName())
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    public void deleteFile(Long fileId) {

        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException("File tidak ditemukan"));

        if (taskFile.getFilePath() != null) {

            try {
                Files.deleteIfExists(
                        Paths.get(taskFile.getFilePath())
                );
            } catch (Exception ignored) {
            }
        }

        taskFileRepository.delete(taskFile);
    }
}