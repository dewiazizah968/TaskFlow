package com.taskflow.service;

import com.taskflow.dto.DashboardResponse;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectMember;
import com.taskflow.dto.MemberInfo;
import com.taskflow.entity.TaskStatus;
import com.taskflow.repository.NotificationRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import com.taskflow.entity.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.taskflow.repository.ProjectMemberRepository;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;
    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;

    public DashboardResponse getDashboardData(User user) {

        List<Project> projects = projectService.getProjectsForUser(user);

        long totalProjects = projects.size();
        long totalTasks = 0;
        long completedTasks = 0;

        List<ProjectSummaryResponse> topProjects = new ArrayList<>();

        for (Project project : projects) {
            long projectTotalTasks = taskRepository.countByProject(project);
            long projectCompletedTasks =
                    taskRepository.countByProjectAndStatus(project, TaskStatus.DONE);

            double projectProgress = 0;

            if (projectTotalTasks > 0) {
                projectProgress =
                        ((double) projectCompletedTasks / projectTotalTasks) * 100;
            }

            String projectStatus = determineProjectStatus(
                    projectTotalTasks,
                    projectCompletedTasks,
                    project
            );

            totalTasks += projectTotalTasks;
            completedTasks += projectCompletedTasks;

            topProjects.add(
                    ProjectSummaryResponse.builder()
                            .projectId(project.getId())
                            .projectName(project.getName())
                            .totalTasks(projectTotalTasks)
                            .completedTasks(projectCompletedTasks)
                            .progressPercentage(projectProgress)
                            .projectStatus(projectStatus)
                            .build()
            );
        }

        double overallProgress = 0;

        if (totalTasks > 0) {
            overallProgress = ((double) completedTasks / totalTasks) * 100;
        }

        long totalNotifications = notificationRepository.countByUserAndIsReadFalse(user);

        long completedProjects = topProjects.stream()
        .filter(project ->
                project.getProjectStatus().equals("DONE"))
        .count();
        
        long runningProjects = topProjects.stream()
        .filter(project -> project.getProjectStatus().equals("IN PROGRESS"))
        .count();
        
        long pendingProjects = topProjects.stream()
        .filter(project -> project.getProjectStatus().equals("NOT YET"))
        .count();
        
        topProjects = topProjects.stream()
                .limit(3)
                .toList();

        List<String> messages = List.of(
                "Keep moving forward.",
                "Small steps still count.",
                "Progress over perfection.",
                "Stay focused, stay consistent.",
                "You are doing great."
        );

        String motivationMessage = messages.get(
                new java.util.Random().nextInt(messages.size())
        );

        Map<Long, MemberInfo> collaborationMembersMap = new java.util.LinkedHashMap<>();
        String[] memberColors = {"#6366f1","#22c55e","#f59e0b","#ef4444","#06b6d4","#a855f7"};

        for (Project project : projects) {
            List<ProjectMember> members =
                    projectMemberRepository.findByProject(project);

            for (ProjectMember member : members) {
                User memberUser = member.getUser();
                if (!memberUser.getId().equals(user.getId())) {
                    String color = memberColors[Math.abs(memberUser.getName().hashCode()) % memberColors.length];
                    collaborationMembersMap.put(memberUser.getId(), MemberInfo.builder()
                            .id(memberUser.getId()).name(memberUser.getName())
                            .avatarColor(color).hasPhoto(memberUser.getProfileImagePath() != null)
                            .build());
                }
            }

            User owner = project.getOwner();
            if (!owner.getId().equals(user.getId())) {
                String color = memberColors[Math.abs(owner.getName().hashCode()) % memberColors.length];
                collaborationMembersMap.put(owner.getId(), MemberInfo.builder()
                        .id(owner.getId()).name(owner.getName())
                        .avatarColor(color).hasPhoto(owner.getProfileImagePath() != null)
                        .build());
            }
        }

        return DashboardResponse.builder()
                .totalProjects(totalProjects)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .progressPercentage(overallProgress)
                .totalNotifications(totalNotifications)
                .topProjects(topProjects)
                .completedProjects(completedProjects)
                .runningProjects(runningProjects)
                .pendingProjects(pendingProjects)
                .motivationMessage(motivationMessage)
                .collaborationMembers(new ArrayList<>(collaborationMembersMap.values()))
                .build();
    }

    private String determineProjectStatus(
            long totalTasks,
            long completedTasks,
            Project project) {

        if (totalTasks == 0) {
            return "NOT YET";
        }

        if (completedTasks == totalTasks) {
            return "DONE";
        }

        long inProgressTasks =
                taskRepository.countByProjectAndStatus(project, TaskStatus.IN_PROGRESS);

        if (inProgressTasks > 0) {
            return "IN PROGRESS";
        }

        return "NOT YET";
    }
}