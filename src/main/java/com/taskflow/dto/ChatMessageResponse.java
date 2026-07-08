package com.taskflow.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private String senderInitial;
    private boolean mine;
    private String content;
    private String createdAt;
}
