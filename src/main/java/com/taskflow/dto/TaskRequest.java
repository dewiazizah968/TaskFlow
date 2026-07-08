package com.taskflow.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequest {

    private String title;
    private String description;
    private String priority;
    private String deadline;

    private Long projectId;
    private Long assignedUserId;
}