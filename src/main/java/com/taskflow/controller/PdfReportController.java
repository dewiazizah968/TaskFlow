package com.taskflow.controller;

import com.taskflow.entity.Project;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.service.PdfReportService;
import com.taskflow.util.PdfReportStyle;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class PdfReportController {

    private final PdfReportService pdfReportService;
    private final ProjectRepository projectRepository;

    @GetMapping("/{projectId}/report")
    public ResponseEntity<byte[]> generateReport(@PathVariable Long projectId) throws Exception {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project tidak ditemukan"));

        byte[] pdf = pdfReportService.generateProjectReport(projectId);

        String filename = "taskflow-projectreport-" + PdfReportStyle.toFileSlug(project.getName()) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
