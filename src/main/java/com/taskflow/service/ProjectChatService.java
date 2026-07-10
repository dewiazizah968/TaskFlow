package com.taskflow.service;

import com.taskflow.dto.ChatMessageRequest;
import com.taskflow.dto.ChatMessageResponse;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectMember;
import com.taskflow.entity.ProjectMessage;
import com.taskflow.entity.User;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectMessageRepository;
import com.taskflow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple project group chat.
 * If a project only has its owner (no other members), the same chat stream
 * is presented on the frontend as a personal "memo" instead of a group chat,
 */
@Service
@RequiredArgsConstructor
public class ProjectChatService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMessageRepository projectMessageRepository;
    private final NotificationService notificationService;

    public ChatMessageResponse sendMessage(Long projectId, User sender, ChatMessageRequest request) {
        Project project = getProjectOrThrow(projectId);
        assertParticipant(project, sender);

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }
        if (content.length() > 2000) {
            throw new RuntimeException("Message is too long (maximum 2000 characters)");
        }

        ProjectMessage message = ProjectMessage.builder()
                .project(project)
                .sender(sender)
                .content(content)
                .build();

        ProjectMessage saved = projectMessageRepository.save(message);

        notifyOtherParticipants(project, sender);

        return toResponse(saved, sender);
    }

    public List<ChatMessageResponse> getHistory(Long projectId, User currentUser) {
        Project project = getProjectOrThrow(projectId);
        assertParticipant(project, currentUser);

        return projectMessageRepository.findByProjectOrderByCreatedAtAsc(project)
                .stream()
                .map(m -> toResponse(m, currentUser))
                .collect(Collectors.toList());
    }

    public List<ChatMessageResponse> getMessagesAfter(Long projectId, Long afterId, User currentUser) {
        Project project = getProjectOrThrow(projectId);
        assertParticipant(project, currentUser);

        long safeAfterId = afterId == null ? 0L : afterId;

        return projectMessageRepository
                .findByProjectAndIdGreaterThanOrderByCreatedAtAsc(project, safeAfterId)
                .stream()
                .map(m -> toResponse(m, currentUser))
                .collect(Collectors.toList());
    }

    private void notifyOtherParticipants(Project project, User sender) {
        List<ProjectMember> members = projectMemberRepository.findByProject(project);
        boolean soloProject = members.isEmpty();

        if (soloProject) {
            // Owner is the only participant: this is just a personal memo, nobody to notify.
            return;
        }

        boolean senderIsOwner = project.getOwner().getId().equals(sender.getId());

        if (!senderIsOwner) {
            notificationService.createChatMessageNotification(project.getOwner(), project, sender);
        }

        for (ProjectMember member : members) {
            User memberUser = member.getUser();
            if (!memberUser.getId().equals(sender.getId())) {
                notificationService.createChatMessageNotification(memberUser, project, sender);
            }
        }
    }

    private void assertParticipant(Project project, User user) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isMember = projectMemberRepository.findByProjectAndUser(project, user).isPresent();

        if (!isOwner && !isMember) {
            throw new RuntimeException("Kamu bukan anggota project ini");
        }
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    private ChatMessageResponse toResponse(ProjectMessage message, User currentUser) {
        User sender = message.getSender();
        String initial = (sender.getName() != null && !sender.getName().isEmpty())
                ? sender.getName().substring(0, 1).toUpperCase()
                : "?";

        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(sender.getId())
                .senderName(sender.getName())
                .senderInitial(initial)
                .mine(sender.getId().equals(currentUser.getId()))
                .content(message.getContent())
                .createdAt(message.getCreatedAt() != null
                        ? message.getCreatedAt().format(TIMESTAMP_FORMAT)
                        : "")
                .build();
    }
}
