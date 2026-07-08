package com.taskflow.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskLinkRequest {
    private String title;
    private String linkUrl;
}