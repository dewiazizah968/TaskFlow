package com.taskflow.service;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskFile;
import com.taskflow.repository.TaskFileRepository;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskFileService {

    private final TaskRepository taskRepository;
    private final TaskFileRepository taskFileRepository;
    private final CloudinaryService cloudinaryService;

    private static final String CLOUDINARY_FOLDER = "taskflow/attachments";

    public List<TaskFile> getFilesByTask(Task task) {
        return taskFileRepository.findByTask(task);
    }

    public TaskFile uploadFile(Long taskId, MultipartFile file) throws Exception {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));

        CloudinaryService.UploadResult uploaded = cloudinaryService.upload(file, CLOUDINARY_FOLDER);

        TaskFile taskFile = TaskFile.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .filePath(uploaded.secureUrl())
                .cloudinaryPublicId(uploaded.publicId())
                .cloudinaryResourceType(uploaded.resourceType())
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

    /**
     * Files now live on Cloudinary, so "downloading" is just redirecting
     * the browser to the file's public HTTPS URL.
     */
    public ResponseEntity<Void> downloadFile(Long fileId) {
        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File tidak ditemukan"));

        if (taskFile.getFilePath() == null) {
            throw new RuntimeException("File tidak ditemukan di Cloudinary");
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(taskFile.getFilePath()))
                .build();
    }

    public void deleteFile(Long fileId) {
        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File tidak ditemukan"));

        if (taskFile.getCloudinaryPublicId() != null) {
            cloudinaryService.delete(taskFile.getCloudinaryPublicId(), taskFile.getCloudinaryResourceType());
        }

        taskFileRepository.delete(taskFile);
    }
}
