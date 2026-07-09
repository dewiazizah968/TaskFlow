package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String fileType;

    private String filePath;

    private String linkUrl;

    private String attachmentType;

    /** Cloudinary public_id for this file, used to delete it from Cloudinary. Null for LINK attachments. */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    /** Cloudinary resource_type ("image", "raw", "video") needed to delete correctly. */
    @Column(name = "cloudinary_resource_type")
    private String cloudinaryResourceType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}