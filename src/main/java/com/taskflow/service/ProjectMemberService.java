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
                        new RuntimeException("Project tidak ditemukan"));

        String email = request.getEmail() == null
                ? null
                : request.getEmail().trim();

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email tidak boleh kosong");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User dengan email tersebut tidak ditemukan"));

        if (user.getId().equals(project.getOwner().getId())) {
            throw new RuntimeException("User tersebut adalah owner project ini");
        }

        boolean memberExists =
                projectMemberRepository
                        .findByProjectAndUser(project, user)
                        .isPresent();

        if (memberExists) {
            throw new RuntimeException(
                    "User sudah menjadi anggota project");
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
                        new RuntimeException("Project tidak ditemukan"));

        return projectMemberRepository.findByProject(project);
    }
}