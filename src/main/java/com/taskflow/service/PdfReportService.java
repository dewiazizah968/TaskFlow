package com.taskflow.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectMember;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.taskflow.util.PdfReportStyle.*;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public byte[] generateProjectReport(Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project tidak ditemukan"));

        List<Task> tasks = taskRepository.findByProject(project);

        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long inProgressTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        double progress = totalTasks == 0 ? 0 : ((double) completedTasks / totalTasks) * 100;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 24, 36);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // ── Banner ──
        document.add(buildBanner(
                "TASKFLOW PROJECT REPORT",
                "Generated " + LocalDateTime.now().format(DATE_FMT)
        ));
        document.add(new Paragraph(" ", body(6f, WHITE)));

        // ── Project info ──
        document.add(sectionHeading("Project Overview"));

        PdfPTable infoTable = new PdfPTable(new float[]{1f, 2f});
        infoTable.setWidthPercentage(100);
        addInfoRow(infoTable, "Project Name", project.getName());
        addInfoRow(infoTable, "Description",
                project.getDescription() == null || project.getDescription().isBlank()
                        ? "-" : project.getDescription());
        addInfoRow(infoTable, "Owner", project.getOwner().getName());
        addInfoRow(infoTable, "Created At",
                project.getCreatedAt() != null ? project.getCreatedAt().format(DATE_FMT) : "-");
        document.add(infoTable);

        // ── Stat cards ──
        document.add(sectionHeading("Summary"));

        PdfPTable stats = new PdfPTable(4);
        stats.setWidthPercentage(100);
        stats.setSpacingBefore(4f);
        stats.addCell(statCard("TOTAL TASKS", String.valueOf(totalTasks), PRIMARY));
        stats.addCell(statCard("COMPLETED", ICON_CHECK + " " + completedTasks, ACCENT_GREEN));
        stats.addCell(statCard("IN PROGRESS", String.valueOf(inProgressTasks), ACCENT_CYAN));
        stats.addCell(statCard("PROGRESS", String.format("%.0f%%", progress), ACCENT_PURPLE));
        document.add(stats);

        document.add(new Paragraph(" ", body(4f, WHITE)));
        document.add(progressBar(progress));

        // ── Task table ──
        document.add(sectionHeading("Task List (" + totalTasks + ")"));

        if (tasks.isEmpty()) {
            document.add(new Paragraph("Belum ada task di project ini.", body(10.5f, TEXT_MUTED)));
        } else {
            PdfPTable table = emptyTableHeader(
                    new String[]{"Title", "Status", "Priority", "Assigned To", "Deadline"},
                    new float[]{2.2f, 1.2f, 1f, 1.4f, 1.3f}
            );

            boolean stripe = false;
            for (Task task : tasks) {
                Color rowBg = stripe ? ROW_STRIPE : WHITE;
                stripe = !stripe;

                table.addCell(tableCell(task.getTitle(), rowBg, TEXT_DARK));

                String statusName = task.getStatus() != null ? task.getStatus().name() : "TODO";
                table.addCell(wrapBadge(statusIcon(statusName) + statusName.replace("_", " "),
                        statusColor(statusName), rowBg));

                String priorityName = task.getPriority() != null ? task.getPriority().name() : "-";
                table.addCell(wrapBadge(priorityIcon(priorityName) + priorityName,
                        priorityColor(priorityName), rowBg));

                String assignedName = task.getAssignedUser() != null
                        ? task.getAssignedUser().getName() : "-";
                table.addCell(tableCell(assignedName, rowBg, TEXT_DARK));

                table.addCell(tableCell(
                        task.getDeadline() != null ? task.getDeadline().format(DATE_FMT) : "-",
                        rowBg, TEXT_MUTED));
            }
            document.add(table);
        }

        // ── Project Members ──
        List<ProjectMember> members = projectMemberRepository.findByProject(project);

        // The owner is shown once, labeled "(Owner)" — if they also exist as a
        // ProjectMember row (e.g. auto-added on project creation), skip that duplicate.
        List<ProjectMember> nonOwnerMembers = members.stream()
                .filter(m -> !m.getUser().getId().equals(project.getOwner().getId()))
                .toList();

        document.add(sectionHeading("Project Members (" + (nonOwnerMembers.size() + 1) + ")"));

        PdfPTable memberTable = emptyTableHeader(
                new String[]{"Name", "Email"},
                new float[]{2f, 3f}
        );

        boolean memberStripe = false;
        Color ownerRowBg = WHITE;
        memberTable.addCell(tableCell(project.getOwner().getName() + "  (Owner)", ownerRowBg, TEXT_DARK));
        memberTable.addCell(tableCell(project.getOwner().getEmail(), ownerRowBg, TEXT_MUTED));
        memberStripe = true;

        for (ProjectMember member : nonOwnerMembers) {
            Color rowBg = memberStripe ? ROW_STRIPE : WHITE;
            memberStripe = !memberStripe;

            memberTable.addCell(tableCell(member.getUser().getName(), rowBg, TEXT_DARK));
            memberTable.addCell(tableCell(member.getUser().getEmail(), rowBg, TEXT_MUTED));
        }
        document.add(memberTable);

        // ── Footer ──
        document.add(new Paragraph(" ", body(10f, WHITE)));
        Paragraph footer = new Paragraph("Generated automatically by TaskFlow \u2022 " +
                LocalDateTime.now().format(DATE_FMT), body(8f, TEXT_MUTED));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return outputStream.toByteArray();
    }
}