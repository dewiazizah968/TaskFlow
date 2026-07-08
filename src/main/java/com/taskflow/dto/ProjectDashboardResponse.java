package com.taskflow.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDashboardResponse {

    private String projectName;

    private long totalTasks;

    private long completedTasks;

    private double progressPercentage;
}