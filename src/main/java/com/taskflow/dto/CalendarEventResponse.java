package com.taskflow.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventResponse {

    private Long taskId;
    private String title;
    private String start;

    private Long projectId;
    private String projectName;

    private String status;
    private String priority;
    private String assignedUser;
    private String deadline;
    private boolean allDay;
    private String color;
}