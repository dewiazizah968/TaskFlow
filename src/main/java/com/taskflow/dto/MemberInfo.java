package com.taskflow.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberInfo {
    private Long id;
    private String name;
    private String avatarColor;
    private boolean hasPhoto;
}
