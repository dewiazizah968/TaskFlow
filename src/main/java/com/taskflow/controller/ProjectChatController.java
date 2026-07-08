package com.taskflow.controller;

import com.taskflow.dto.ChatMessageRequest;
import com.taskflow.dto.ChatMessageResponse;
import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/chat")
@RequiredArgsConstructor
public class ProjectChatController {

    private final ProjectChatService projectChatService;
    private final UserRepository userRepository;

    /** Full chat/memo history, oldest first. */
    @GetMapping("/messages")
    public List<ChatMessageResponse> getHistory(
            @PathVariable Long projectId,
            Authentication authentication) {

        return projectChatService.getHistory(projectId, getUser(authentication));
    }

    /** Lightweight polling endpoint: only messages posted after a given id. */
    @GetMapping("/messages/after/{afterId}")
    public List<ChatMessageResponse> getMessagesAfter(
            @PathVariable Long projectId,
            @PathVariable Long afterId,
            Authentication authentication) {

        return projectChatService.getMessagesAfter(projectId, afterId, getUser(authentication));
    }

    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(
            @PathVariable Long projectId,
            @RequestBody ChatMessageRequest request,
            Authentication authentication) {

        return projectChatService.sendMessage(projectId, getUser(authentication), request);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
    }
}
