package com.taskflow.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {
    private Long id;
    private String message;
    private boolean isRead;
    private String type;
    private Long taskId;
    private String taskTitle;
    private Long projectId;
    private String projectName;
    private String createdAt;
}
