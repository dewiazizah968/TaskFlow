package com.taskflow.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.taskflow.entity.*;
import com.taskflow.repository.TaskHistoryRepository;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static com.taskflow.util.PdfReportStyle.*;

@Service
@RequiredArgsConstructor
public class UserDataExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final ProjectService projectService;
    private final TaskRepository taskRepository;
    private final TaskHistoryRepository taskHistoryRepository;

    public byte[] exportUserProjects(User user) throws Exception {
        List<Project> projects = projectService.getProjectsForUser(user);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 24, 36);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // ── Banner ──
        document.add(buildBanner(
                "TASKFLOW DATA REPORT",
                "Generated for " + user.getName() + " \u2022 " + LocalDateTime.now().format(DATE_FMT)
        ));
        document.add(new Paragraph(" ", body(6f, WHITE)));

        // ── Overall summary across all projects ──
        int totalProjects = projects.size();
        long totalTasksAll = 0;
        long completedTasksAll = 0;
        for (Project project : projects) {
            List<Task> tasks = taskRepository.findByProject(project);
            totalTasksAll += tasks.size();
            completedTasksAll += tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        }

        document.add(sectionHeading("Summary"));
        PdfPTable stats = new PdfPTable(3);
        stats.setWidthPercentage(100);
        stats.addCell(statCard("PROJECTS", String.valueOf(totalProjects), PRIMARY));
        stats.addCell(statCard("TOTAL TASKS", String.valueOf(totalTasksAll), ACCENT_CYAN));
        stats.addCell(statCard("COMPLETED", ICON_CHECK + " " + completedTasksAll, ACCENT_GREEN));
        document.add(stats);

        if (projects.isEmpty()) {
            document.add(new Paragraph(" ", body(8f, WHITE)));
            document.add(new Paragraph("Belum ada project.", body(10.5f, TEXT_MUTED)));
        }

        for (Project project : projects) {
            List<Task> tasks = taskRepository.findByProject(project);

            document.add(sectionHeading(project.getName()));

            PdfPTable infoTable = new PdfPTable(new float[]{1f, 2f});
            infoTable.setWidthPercentage(100);
            addInfoRow(infoTable, "Description",
                    project.getDescription() == null || project.getDescription().isBlank()
                            ? "-" : project.getDescription());
            addInfoRow(infoTable, "Owner", project.getOwner().getName());
            addInfoRow(infoTable, "Created At",
                    project.getCreatedAt() != null ? project.getCreatedAt().format(DATE_FMT) : "-");

            LocalDateTime completedAt = getProjectCompletedAt(tasks);
            addInfoRow(infoTable, "Completed At",
                    completedAt != null ? completedAt.format(DATE_FMT) : "-");
            document.add(infoTable);

            if (tasks.isEmpty()) {
                Paragraph none = new Paragraph("Belum ada task.", body(9.5f, TEXT_MUTED));
                none.setSpacingBefore(4f);
                none.setSpacingAfter(10f);
                document.add(none);
                continue;
            }

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

            table.setSpacingAfter(14f);
            document.add(table);
        }

        // ── Footer ──
        document.add(new Paragraph(" ", body(6f, WHITE)));
        Paragraph footer = new Paragraph("Generated automatically by TaskFlow \u2022 " +
                LocalDateTime.now().format(DATE_FMT), body(8f, TEXT_MUTED));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return outputStream.toByteArray();
    }

    private LocalDateTime getProjectCompletedAt(List<Task> tasks) {
        if (tasks.isEmpty()) return null;

        boolean allDone = tasks.stream().allMatch(task -> task.getStatus() == TaskStatus.DONE);
        if (!allDone) return null;

        return tasks.stream()
                .flatMap(task -> taskHistoryRepository.findByTask(task).stream())
                .filter(history -> "DONE".equals(history.getNewValue()))
                .map(TaskHistory::getChangedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
