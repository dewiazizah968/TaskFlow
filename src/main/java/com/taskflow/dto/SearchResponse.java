package com.taskflow.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @AllArgsConstructor
public class SearchResponse {
    private String keyword;
    private List<ProjectItem> projects;
    private List<TaskItem> tasks;

    @Getter @AllArgsConstructor
    public static class ProjectItem {
        private Long id;
        private String name;
        private String description;
    }

    @Getter @AllArgsConstructor
    public static class TaskItem {
        private Long id;
        private String title;
        private String description;
        private String status;
        private Long projectId;
        private String projectName;
    }
}
