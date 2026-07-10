package com.taskflow.service;

import com.taskflow.dto.AddMemberRequest;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectMember;
import com.taskflow.entity.User;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationService notificationService;

    public ProjectMember addMember(
            Long projectId,
            AddMemberRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        String email = request.getEmail() == null
                ? null
                : request.getEmail().trim();

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User with that email was not found"));

        if (user.getId().equals(project.getOwner().getId())) {
            throw new RuntimeException("This user is the owner of this project");
        }

        boolean memberExists =
                projectMemberRepository
                        .findByProjectAndUser(project, user)
                        .isPresent();

        if (memberExists) {
            throw new RuntimeException(
                    "User is already a member of the project");
        }

        ProjectMember projectMember =
                ProjectMember.builder()
                        .project(project)
                        .user(user)
                        .build();

        ProjectMember saved = projectMemberRepository.save(projectMember);

        notificationService.createProjectInviteNotification(user, project);

        return saved;
    }

    public List<ProjectMember> getMembersByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        return projectMemberRepository.findByProject(project);
    }
}