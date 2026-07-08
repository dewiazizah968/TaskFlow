package com.taskflow.controller;

import com.taskflow.dto.AddMemberRequest;
import com.taskflow.entity.ProjectMember;
import com.taskflow.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping("/{projectId}/members")
    public ProjectMember addMember(
            @PathVariable Long projectId,
            @RequestBody AddMemberRequest request) {

        return projectMemberService.addMember(
                projectId,
                request
        );
    }

    @GetMapping("/{projectId}/members")
    public List<ProjectMember> getMembersByProject(
            @PathVariable Long projectId) {

        return projectMemberService.getMembersByProject(projectId);
    }
}