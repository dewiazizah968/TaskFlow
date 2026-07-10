package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.UserDataExportService;
import com.taskflow.util.PdfReportStyle;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final UserDataExportService userDataExportService;

    @GetMapping("/dashboard/export")
    public ResponseEntity<ByteArrayResource> exportUserData(
            Authentication authentication) throws Exception {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        byte[] pdfData = userDataExportService.exportUserProjects(user);

        ByteArrayResource resource = new ByteArrayResource(pdfData);

        String filename = "taskflow-datareport-" + PdfReportStyle.toFileSlug(user.getName()) + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfData.length)
                .body(resource);
    }
}
