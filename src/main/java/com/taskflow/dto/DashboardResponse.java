package com.taskflow.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private long totalProjects;
    private long totalTasks;
    private long completedTasks;
    private double progressPercentage;
    private long totalNotifications;

    private long completedProjects;
    private long runningProjects;
    private long pendingProjects;
    private String motivationMessage;

    private List<ProjectSummaryResponse> topProjects;
    private List<MemberInfo> collaborationMembers;
}